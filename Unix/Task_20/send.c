#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
#include <sys/msg.h>
#include <memory.h>

#define MAX_SEND_SZ 32
#define DATA_TYPE 1
#define REG_TYPE 2
#define END_TYPE 3

struct msgbuf {
    long mtype;
    pid_t pid;
    char mtext[MAX_SEND_SZ];
};

int main() {
    int queue;
    if ((queue = msgget(getuid(), 0)) == -1) {
        perror("msgget");
        return 1;
    }
    struct msgbuf buf = {REG_TYPE, getpid(), {0}};
    if (msgsnd(queue, &buf, sizeof(pid_t), 0) == -1) {
        perror("msgsend");
        return 1;
    }
    while (1) {
        if (!fgets(buf.mtext, MAX_SEND_SZ, stdin)) {
            buf.mtype = END_TYPE;
            if (msgsnd(queue, &buf, sizeof(pid_t), 0) == -1) {
                perror("msgsend");
                return 1;
            }
            break;
        } else {
            buf.mtype = DATA_TYPE;
            if (msgsnd(queue, &buf, strlen(buf.mtext) + sizeof(pid_t) + 1, 0) == -1) {
                perror("msgsend");
                return 1;
            }
        }
    }
    return 0;
}
