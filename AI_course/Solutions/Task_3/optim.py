import numpy as np

class SGD:
    def update(self, w, d_w, learning_rate):
        return w - d_w * learning_rate


class MomentumSGD:
    """
    Implements Momentum SGD update
    """
    def __init__(self, momentum=0.9):
        self.momentum = momentum
        self.velocity = None


    def update(self, w, d_w, learning_rate):
        """
        Performs Momentum SGD update

        Arguments:
            w, np array - weights
            d_w, np array, same shape as w - gradient
            learning_rate, float - learning rate

        Returns:
            updated_weights, np array same shape as w
        """
        # TODO: Copy from the previous assignment
        if self.velocity is None:
            self.velocity = np.zeros_like(w)
        self.velocity = self.momentum * self.velocity - learning_rate * d_w
        return w + self.velocity
