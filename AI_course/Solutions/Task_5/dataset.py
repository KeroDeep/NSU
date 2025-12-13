import os

class Dataset:
    def __init__(self):
        self.index_by_token = {}
        self.token_by_index = []

        self.sentences = []

        self.token_freq = []

    def load_dataset(self, folder):
        filename = os.path.join(folder, "datasetSentences.txt")

        with open(filename, "r", encoding="latin1") as f:
            next(f)  # Skip header line
            for line in f:
                parts = line.strip().split('\t')
                if len(parts) != 2:
                    continue
                sentence_id, sentence = parts
                tokens = sentence.split()
                self.sentences.append(tokens)
