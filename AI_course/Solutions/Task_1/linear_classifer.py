import numpy as np

def softmax(predictions):
    """
    Computes probabilities from scores

    Arguments:
        predictions, np array, shape is either (N) or (batch_size, N) - classifier output

    Returns:
        probs, np array of the same shape as predictions - probability for every class, 0..1
    """
    # TODO: Implement softmax
    # Your final implementation shouldn't have any loops
    if predictions.ndim == 1:
        predictions = predictions.reshape(1, -1)
    max_pred = np.max(predictions, axis=1, keepdims=True)
    exp_pred = np.exp(predictions - max_pred)
    probs = exp_pred / np.sum(exp_pred, axis=1, keepdims=True)
    return probs.reshape(predictions.shape)


def cross_entropy_loss(probs, target_index):
    """
    Computes cross-entropy loss

    Arguments:
        probs, np array, shape is either (N) or (batch_size, N) - probabilities for every class
        target_index: np array of int, shape is (1) or (batch_size) - index of the true class for given sample(s)

    Returns:
        loss: single value
    """
    # TODO: Implement cross-entropy
    # Your final implementation shouldn't have any loops
    if probs.ndim == 1:
        probs = probs.reshape(1, -1)
    if isinstance(target_index, int):
        target_index = np.array([target_index])
    if target_index.ndim == 0:
        target_index = target_index.reshape(1)
    batch_size = probs.shape[0]
    loss = -np.log(probs[np.arange(batch_size), target_index]).mean()
    return loss


def softmax_with_cross_entropy(predictions, target_index):
    """
    Computes softmax and cross-entropy loss for model predictions,
    including the gradient

    Arguments:
        predictions, np array, shape is either (N) or (batch_size, N) - classifier output
        target_index: np array of int, shape is (1) or (batch_size) - index of the true class for given sample(s)

    Returns:
        loss, single value - cross-entropy loss
        dprediction, np array same shape as predictions - gradient of predictions by loss value
    """
    # TODO: Implement softmax with cross-entropy
    # Your final implementation shouldn't have any loops
    if predictions.ndim == 1:
        predictions = predictions.reshape(1, -1)
    if isinstance(target_index, int):
        target_index = np.array([target_index])
    if target_index.ndim == 0:
        target_index = target_index.reshape(1)

    batch_size = predictions.shape[0]
    max_pred = np.max(predictions, axis=1, keepdims=True)
    shifted = predictions - max_pred
    exp_shifted = np.exp(shifted)
    probs = exp_shifted / np.sum(exp_shifted, axis=1, keepdims=True)

    loss = -np.log(probs[np.arange(batch_size), target_index]).mean()

    dprediction = probs.copy()
    dprediction[np.arange(batch_size), target_index] -= 1
    dprediction /= batch_size

    if dprediction.shape[0] == 1:
        dprediction = dprediction.flatten()

    return loss, dprediction


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
    # TODO: Implement l2 regularization and gradient
    # Your final implementation shouldn't have any loops
    loss = reg_strength * np.sum(W ** 2)
    grad = 2 * reg_strength * W
    return loss, grad
    

def linear_softmax(X, W, target_index):
    """
    Performs linear classification and returns loss and gradient over W

    Arguments:
        X, np array, shape (num_batch, num_features) - batch of images
        W, np array, shape (num_features, classes) - weights
        target_index, np array, shape (num_batch) - index of target classes

    Returns:
        loss, single value - cross-entropy loss
        gradient, np.array same shape as W - gradient of weight by loss
    """
    predictions = np.dot(X, W)

    # TODO: Implement prediction and gradient over W
    # Your final implementation shouldn't have any loops
    loss, dpred = softmax_with_cross_entropy(predictions, target_index)
    dW = np.dot(X.T, dpred)

    return loss, dW


class LinearSoftmaxClassifier():
    def __init__(self):
        self.W = None


    def fit(self, X, y, batch_size=100, learning_rate=1e-7, reg=1e-5, epochs=1):
        """
        Trains linear classifier

        Arguments:
            X, np array (num_samples, num_features) - training data
            y, np array of int (num_samples) - labels
            batch_size, int - batch size to use
            learning_rate, float - learning rate for gradient descent
            reg, float - L2 regularization strength
            epochs, int - number of epochs
        """
        num_train = X.shape[0]
        num_features = X.shape[1]
        num_classes = np.max(y)+1
        if self.W is None:
            self.W = 0.001 * np.random.randn(num_features, num_classes)

        loss_history = []
        for epoch in range(epochs):
            shuffled_indices = np.arange(num_train)
            np.random.shuffle(shuffled_indices)
            sections = np.arange(batch_size, num_train, batch_size)
            batches_indices = np.array_split(shuffled_indices, sections)

            # TODO: Implement generating batches from indices
            # Compute loss and gradients
            # Apply gradient to weights using learning rate
            # Don't forget to add both cross-entropy loss and regularization!
            total_loss = 0
            for batch in batches_indices:
                X_batch = X[batch]
                y_batch = y[batch]
                loss, dW = linear_softmax(X_batch, self.W, y_batch)
                reg_loss, reg_grad = l2_regularization(self.W, reg)
                total_loss += loss + reg_loss
                self.W -= learning_rate * (dW + reg_grad)
            avg_loss = total_loss / len(batches_indices)
            loss_history.append(avg_loss)

            # End
            print("Epoch %i, loss: %f" % (epoch, avg_loss))

        return loss_history


    def predict(self, X):
        """
        Produces classifier predictions on the set

        Arguments:
            X, np array (test_samples, num_features)

        Returns:
            y_pred, np.array of int (test_samples)
        """
        y_pred = np.zeros(X.shape[0], dtype=np.int)

        # TODO: Implement class prediction
        # Your final implementation shouldn't have any loops
        predictions = np.dot(X, self.W)
        y_pred = np.argmax(predictions, axis=1)

        return y_pred
