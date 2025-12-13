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


def softmax_with_cross_entropy(predictions, target_index):
    """
    Computes softmax and cross-entropy loss for model predictions, including the gradient

    Arguments:
        predictions, np array, shape is either (N) or (batch_size, N) - classifier output
        target_index: np array of int, shape is (1) or (batch_size) - index of the true class for given sample(s)

    Returns:
        loss, single value - cross-entropy loss
        dprediction, np array same shape as predictions - gradient of predictions by loss value
    """
    if predictions.ndim == 1:
        predictions = predictions.reshape(1, -1)
        if isinstance(target_index, int):
            target_index = np.array([target_index])
        elif np.isscalar(target_index):
            target_index = np.array([target_index])

    batch_size = predictions.shape[0]
    max_pred = np.max(predictions, axis=1, keepdims=True)
    shifted = predictions - max_pred
    exp_shifted = np.exp(shifted)
    probs = exp_shifted / np.sum(exp_shifted, axis=1, keepdims=True)

    if target_index.ndim == 0:
        target_index = np.array([target_index])
    if target_index.ndim == 1 and target_index.size == 1 and batch_size > 1:
        target_index = np.full(batch_size, target_index.item())

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
        d_result = d_out * (self.X > 0)
        return d_result


    def params(self):
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
        batch_size = d_out.shape[0]

        d_input = np.dot(d_out, self.W.value.T)
        self.W.grad = np.dot(self.X.T, d_out)
        self.B.grad = np.sum(d_out, axis=0, keepdims=True)

        return d_input


    def params(self):
        return { "W": self.W, "B": self.B }


class ConvolutionalLayer:
    def __init__(self, in_channels, out_channels, filter_size, padding):
        """
        Initializes the layer

        Arguments:
            in_channels, int - number of input channels
            out_channels, int - number of output channels
            filter_size, int - size of the conv filter
            padding, int - number of «pixels» to pad on each side
        """

        self.filter_size = filter_size
        self.in_channels = in_channels
        self.out_channels = out_channels
        self.W = Param(np.random.randn(filter_size, filter_size, in_channels, out_channels) * 0.001)

        self.B = Param(np.zeros(out_channels))

        self.padding = padding


    def forward(self, X):
        batch_size, height, width, channels = X.shape

        if self.padding > 0:
            X_padded = np.zeros((batch_size, height + 2 * self.padding, width + 2 * self.padding, channels))
            X_padded[:, self.padding:height + self.padding, self.padding:width + self.padding, :] = X
        else:
            X_padded = X

        self.X = X_padded

        out_height = (height - self.filter_size + 2 * self.padding) + 1
        out_width = (width - self.filter_size + 2 * self.padding) + 1

        output = np.zeros((batch_size, out_height, out_width, self.out_channels))

        # TODO: Implement forward pass
        # NOTE: Setup variables that hold the result and one x/y location at a time in the loop below

        # It's ok to use loops for going over width and height but try to avoid having any other loops
        for y in range(out_height):
            for x in range(out_width):
                # TODO: Implement forward pass for specific location
                x_window = self.X[:, y:y + self.filter_size, x:x + self.filter_size, :]
                x_flat = x_window.reshape(batch_size, -1)
                w_flat = self.W.value.reshape(-1, self.out_channels)
                output[:, y, x, :] = np.dot(x_flat, w_flat) + self.B.value

        return output


    def backward(self, d_out):
        # NOTE: Forward pass was reduced to matrix multiply
        # You already know how to backprop through that when you implemented FullyConnectedLayer
        # Just do it the same number of times and accumulate gradients

        batch_size, height, width, channels = self.X.shape
        _, out_height, out_width, out_channels = d_out.shape

        d_input_padded = np.zeros_like(self.X)
        self.W.grad = np.zeros_like(self.W.value)
        self.B.grad = np.sum(d_out, axis=(0, 1, 2))

        # TODO: Implement backward pass
        # Same as forward, setup variables of the right shape that aggregate input gradient and fill them for every location of the output

        # Try to avoid having any other loops here too
        for y in range(out_height):
            for x in range(out_width):
                # TODO: Implement backward pass for specific location
                # Aggregate gradients for both the input and the parameters (W and B)
                x_window = self.X[:, y:y + self.filter_size, x:x + self.filter_size, :]
                grad = d_out[:, y, x, :].reshape(batch_size, 1, 1, 1, out_channels)
                x_window_exp = x_window[:, :, :, :, np.newaxis]
                self.W.grad += np.sum(x_window_exp * grad, axis=0)

                w_exp = self.W.value[np.newaxis, :, :, :, :]
                d_out_exp = d_out[:, y, x, :][:, np.newaxis, np.newaxis, np.newaxis, :]
                d_input_padded[:, y:y + self.filter_size, x:x + self.filter_size, :] += \
                    np.sum(w_exp * d_out_exp, axis=4)

        if self.padding > 0:
            d_input = d_input_padded[:, self.padding:-self.padding, self.padding:-self.padding, :]
        else:
            d_input = d_input_padded

        return d_input


    def params(self):
        return { "W": self.W, "B": self.B }


class MaxPoolingLayer:
    def __init__(self, pool_size, stride):
        """
        Initializes the max pool

        Arguments:
            pool_size, int - area to pool
            stride, int - step size between pooling windows
        """
        self.pool_size = pool_size
        self.stride = stride
        self.X = None
        self.mask = None


    def forward(self, X):
        batch_size, height, width, channels = X.shape
        # TODO: Implement maxpool forward pass
        # NOTE: Similarly to Conv layer, loop on output x/y dimension
        self.X = X

        out_height = (height - self.pool_size) // self.stride + 1
        out_width = (width - self.pool_size) // self.stride + 1

        output = np.zeros((batch_size, out_height, out_width, channels))
        self.mask = np.zeros_like(X)

        for y in range(out_height):
            for x in range(out_width):
                y_start = y * self.stride
                y_end = y_start + self.pool_size
                x_start = x * self.stride
                x_end = x_start + self.pool_size

                window = X[:, y_start:y_end, x_start:x_end, :]
                output[:, y, x, :] = np.max(window, axis=(1, 2))

                max_mask = (window == output[:, y:y+1, x:x+1, :])
                self.mask[:, y_start:y_end, x_start:x_end, :] += max_mask

        return output


    def backward(self, d_out):
        # TODO: Implement maxpool backward pass
        batch_size, height, width, channels = self.X.shape
        _, out_height, out_width, _ = d_out.shape

        d_input = np.zeros_like(self.X)

        for y in range(out_height):
            for x in range(out_width):
                y_start = y * self.stride
                y_end = y_start + self.pool_size
                x_start = x * self.stride
                x_end = x_start + self.pool_size

                grad = d_out[:, y:y+1, x:x+1, :]
                d_input[:, y_start:y_end, x_start:x_end, :] += grad * self.mask[:, y_start:y_end, x_start:x_end, :]

        return d_input


    def params(self):
        return {}


class Flattener:
    def __init__(self):
        self.X_shape = None


    def forward(self, X):
        batch_size, height, width, channels = X.shape

        # TODO: Implement forward pass
        # Layer should return array with dimensions [batch_size, hight*width*channels]
        self.X_shape = X.shape
        return X.reshape(batch_size, -1)


    def backward(self, d_out):
        # TODO: Implement backward pass
        return d_out.reshape(self.X_shape)


    def params(self):
        # No params!
        return {}
