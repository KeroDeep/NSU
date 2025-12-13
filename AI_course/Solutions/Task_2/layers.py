import numpy as np

def l2_regularization(W, reg_strength):
    """
    Computes L2 regularization loss on weights and its gradient

    Arguments:
        W, np array - weights
        reg_strength - float value

    Returns:
        loss, single value - l2 regularization loss
        gradient, np.array same shape as W - gradient of weight by l2 loss
    """
    loss = reg_strength * np.sum(W ** 2)
    grad = 2 * reg_strength * W
    return loss, grad


def softmax_with_cross_entropy(preds, target_index):
    """
    Computes softmax and cross-entropy loss for model predictions, including the gradient

    Arguments:
        predictions, np array, shape is either (N) or (batch_size, N) - classifier output
        target_index: np array of int, shape is (1) or (batch_size) - index of the true class for given sample(s)

    Returns:
        loss, single value - cross-entropy loss
        dprediction, np array same shape as predictions - gradient of predictions by loss value
    """
    if preds.ndim == 1:
        preds = preds.reshape(1, -1)
        target_index = np.array([target_index])

    batch_size = preds.shape[0]
    max_pred = np.max(preds, axis=1, keepdims=True)
    shifted = preds - max_pred
    exp_shifted = np.exp(shifted)
    probs = exp_shifted / np.sum(exp_shifted, axis=1, keepdims=True)

    if target_index.ndim == 0:
        target_index = np.array([target_index])
    if target_index.ndim == 1 and target_index.size == 1:
        target_index = np.array([target_index])

    # Extract log probabilities for the correct classes
    log_probs = -np.log(probs[np.arange(batch_size), target_index])
    loss = np.mean(log_probs)

    dprediction = probs.copy()
    dprediction[np.arange(batch_size), target_index] -= 1
    dprediction /= batch_size

    if dprediction.shape[0] == 1:
        dprediction = dprediction.flatten()

    return loss, dprediction


class Param:
    """
    Trainable parameter of the model
    Captures both parameter value and the gradient
    """
    def __init__(self, value):
        self.value = value
        self.grad = np.zeros_like(value)


class ReLULayer:
    def __init__(self):
        self.X = None


    def forward(self, X):
        self.X = X
        return np.maximum(0, X)


    def backward(self, d_out):
        """
        Backward pass

        Arguments:
            d_out, np array (batch_size, num_features) - gradient of loss function with respect to output

        Returns:
            d_result: np array (batch_size, num_features) - gradient with respect to input
        """
        d_result = d_out * (self.X > 0)
        return d_result


    def params(self):
        # ReLU Doesn't have any parameters
        return {}


class FullyConnectedLayer:
    def __init__(self, n_input, n_output):
        self.W = Param(0.001 * np.random.randn(n_input, n_output))
        self.B = Param(0.001 * np.random.randn(1, n_output))
        self.X = None


    def forward(self, X):
        self.X = X
        return np.dot(X, self.W.value) + self.B.value


    def backward(self, d_out):
        """
        Backward pass
        Computes gradient with respect to input and accumulates gradients within self.W and self.B

        Arguments:
            d_out, np array (batch_size, n_output) - gradient of loss function with respect to output

        Returns:
            d_result: np array (batch_size, n_input) - gradient with respect to input
        """
        batch_size = d_out.shape[0]

        # Gradient w.r.t input
        d_input = np.dot(d_out, self.W.value.T)

        # Gradient w.r.t weights
        self.W.grad = np.dot(self.X.T, d_out)

        # Gradient w.r.t bias
        self.B.grad = np.sum(d_out, axis=0, keepdims=True)

        return d_input


    def params(self):
        return {"W": self.W, "B": self.B}
