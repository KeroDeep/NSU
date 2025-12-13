import numpy as np

from layers import FullyConnectedLayer, ReLULayer, softmax_with_cross_entropy, l2_regularization

class TwoLayerNet:
    """
    Neural network with two fully connected layers
    """
    def __init__(self, n_input, n_output, hidden_layer_size, reg):
        """
        Initializes the neural network

        Arguments:
            n_input, int - dimension of the model input
            n_output, int - number of classes to predict
            hidden_layer_size, int - number of neurons in the hidden layer
            reg, float - L2 regularization strength
        """
        self.reg = reg
        # TODO: Create necessary layers
        self.fc1 = FullyConnectedLayer(n_input, hidden_layer_size)
        self.relu = ReLULayer()
        self.fc2 = FullyConnectedLayer(hidden_layer_size, n_output)


    def compute_loss_and_gradients(self, X, y):
        """
        Computes total loss and updates parameter gradients on a batch of training examples

        Arguments:
            X, np array (batch_size, input_features) - input data
            y, np array of int (batch_size) - classes
        """
        # Before running forward and backward pass through the model, clear parameter gradients aggregated from the previous pass
        # TODO: Set parameter gradient to zeros
        # NOTE: Using self.params() might be useful!
        for param in self.params().values():
            param.grad = np.zeros_like(param.grad)

        # Forward pass
        out1 = self.fc1.forward(X)
        out2 = self.relu.forward(out1)
        out3 = self.fc2.forward(out2)

        # Compute loss and gradient from softmax + cross-entropy
        loss, d_out3 = softmax_with_cross_entropy(out3, y)

        # Backward pass
        d_out2 = self.fc2.backward(d_out3)
        d_out1 = self.relu.backward(d_out2)
        self.fc1.backward(d_out1)

        # After that, implement l2 regularization on all params
        # NOTE: self.params() is useful again!
        for param in self.params().values():
            l2_loss, l2_grad = l2_regularization(param.value, self.reg)
            loss += l2_loss
            param.grad += l2_grad

        return loss


    def predict(self, X):
        """
        Produces classifier predictions on the set

        Arguments:
            X, np array (test_samples, num_features)

        Returns:
            y_pred, np.array of int (test_samples)
        """
        # TODO: Implement predict
        # NOTE: Some of the code of the compute_loss_and_gradients can be reused
        out1 = self.fc1.forward(X)
        out2 = self.relu.forward(out1)
        out3 = self.fc2.forward(out2)
        pred = np.argmax(out3, axis=1)

        return pred


    def params(self):
        result = {
            'fc1.W': self.fc1.W,
            'fc1.B': self.fc1.B,
            'fc2.W': self.fc2.W,
            'fc2.B': self.fc2.B
        }

        return result
