#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
#include <sys/msg.h>
#include <memory.h>

#define MAX_RCV_SZ 32
#define DATA_TYPE 1
#define REG_TYPE 2
#define END_TYPE 3

struct msgbuf {
    long mtype;
    pid_t pid;
    char mtext[MAX_RCV_SZ];
};

int main() {
    int queue;
    if ((queue = msgget(getuid(), IPC_CREAT | 0660)) == -1) {
        perror("msgget");
        return -1;
    }
    struct msgbuf buf;
    int senders = 0;
    while (1) {
        if (msgrcv(queue, &buf, MAX_RCV_SZ + sizeof(pid_t), REG_TYPE, IPC_NOWAIT) != -1)
            senders++;
        if (msgrcv(queue, &buf, MAX_RCV_SZ + sizeof(pid_t), DATA_TYPE, IPC_NOWAIT) != -1)
            printf("%d: %s", buf.pid, buf.mtext);
        if (msgrcv(queue, &buf, MAX_RCV_SZ + sizeof(pid_t), END_TYPE, IPC_NOWAIT) != -1)
            if (--senders == 0)
                break;
    }
    if (msgctl(queue, IPC_RMID, NULL) == -1) {
        perror("msgctl");
        return 1;
    }
    return 0;
}
