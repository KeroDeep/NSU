def binary_classification_metrics(prediction, ground_truth):
    precision = 0
    recall = 0
    accuracy = 0
    f1 = 0

    # TODO: Implement metrics!
    tp = ((prediction == True) & (ground_truth == True)).sum()
    fp = ((prediction == True) & (ground_truth == False)).sum()
    fn = ((prediction == False) & (ground_truth == True)).sum()
    tn = ((prediction == False) & (ground_truth == False)).sum()

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

    if tp + tn + fp + fn > 0:
        accuracy = (tp + tn) / (tp + tn + fp + fn)
    else:
        accuracy = 0.0

    return accuracy, precision, recall, f1


def multiclass_accuracy(prediction, ground_truth):
    return (prediction == ground_truth).mean()
