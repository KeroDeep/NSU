import numpy as np

def binary_classification_metrics(prediction, ground_truth):
    """
    Computes metrics for binary classification

    Arguments:
        prediction, np array of bool (num_samples) - model predictions
        ground_truth, np array of bool (num_samples) - true labels

    Returns:
        precision, recall, f1, accuracy - classification metrics
    """
    precision = 0
    recall = 0
    accuracy = 0
    f1 = 0

    # TODO: Implement metrics!
    # Some helpful links:
    # https://en.wikipedia.org/wiki/Precision_and_recall
    # https://en.wikipedia.org/wiki/F1_score

    tp = np.sum((prediction == True) & (ground_truth == True))
    fp = np.sum((prediction == True) & (ground_truth == False))
    fn = np.sum((prediction == False) & (ground_truth == True))
    tn = np.sum((prediction == False) & (ground_truth == False))

    if tp + fp > 0:
        precision = tp / (tp + fp)
    else:
        precision = 0.0

    if tp + fn > 0:
        recall = tp / (tp + fn)
    else:
        recall = 0.0

    if precision + recall > 0:
        f1 = 2 * precision * recall / (precision + recall)
    else:
        f1 = 0.0

    accuracy = (tp + tn) / (tp + tn + fp + fn)

    return precision, recall, f1, accuracy


def multiclass_accuracy(prediction, ground_truth):
    """
    Computes metrics for multiclass classification

    Arguments:
        prediction, np array of int (num_samples) - model predictions
        ground_truth, np array of int (num_samples) - true labels

    Returns:
        accuracy - ratio of accurate predictions to total samples
    """
    # TODO: Implement computing accuracy
    return np.mean(prediction == ground_truth)
