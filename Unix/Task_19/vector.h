#include <stdlib.h>
#include <stdio.h>

struct vector {
    pid_t *vec;
    int firstFree;
    int size;
};

struct vector *initVector() {
    struct vector *res = calloc(1, sizeof(struct vector));
    if (res == NULL) {
        perror("calloc");
        return NULL;
    }
    return res;
}

int addItem(struct vector *vec, pid_t item) {
    if (vec->firstFree < vec->size) {
        vec->vec[vec->firstFree] = item;
        ++vec->firstFree;
        return 0;
    }
    pid_t *tmp = (pid_t *) realloc(vec->vec, vec->size + 1);
    if (tmp == NULL) {
        perror("realloc");
        return 1;
    }
    vec->size += 1;
    vec->vec = tmp;
    vec->vec[vec->firstFree] = item;
    ++vec->firstFree;
    return 0;
}

int findItem(struct vector *vec, pid_t item) {
    for (int i = 0; i < vec->firstFree; ++i) {
        if (vec->vec[i] == item) {
            return i;
        }
    }
    return -1;
}

int removeItem(struct vector *vec, int index) {
    if (index < 0 || index >= vec->firstFree) {
        return 1;
    }
    for (int i = index + 1; i < vec->firstFree; ++i) {
        vec->vec[i - 1] = vec->vec[i];
    }
    --vec->firstFree;
    return 0;
}

void destructorVector(struct vector *vec) {
    free(vec->vec);
    free(vec);
}
