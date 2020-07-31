#include <semaphore.h>
#include <sys/types.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <sys/mman.h>
#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
#include <pthread.h>
#include <string.h>

const char* input = "input.db";
const char* output = "output.db";

int client1_pid;
int client2_pid;

void client_1(void);
void client_2(void);

sem_t* canStart;
sem_t* toServerClient2;

sem_t* getClient1;
sem_t* getClient2;
sem_t* init;
sem_t* fromServer2;
sem_t* clientDone;
sem_t* serverDone;
sem_t* done_flag;

sem_t* writeNum;
sem_t* writeSign;
sem_t* emptyNum;
sem_t* emptySign;

sem_t* enterClient1;
sem_t* enterClient2;
sem_t* toClient1;
sem_t* storeClient2;
sem_t* forClient1;
sem_t* forClient2;

int* ptr_input;
int* ptr_output;

int main(void){

	int status;
	int fd_output;
	int fd_input;
	pthread_t pt[2];
	
	//shared with Clients
	sem_unlink("forClient1");
	sem_unlink("forClient2");
	sem_unlink("enterClient1");
	sem_unlink("enterClient2");
	sem_unlink("toClient1");
	sem_unlink("storeClient2");
	sem_unlink("getClient1");	
	sem_unlink("getClient2");
	sem_unlink("init");
	sem_unlink("toServerClient2");	
	sem_unlink("emptySign");	
	sem_unlink("emptyNum");	
	sem_unlink("writeNum");	
	sem_unlink("writeSign");	
	sem_unlink("canStart");	
	sem_unlink("fromServer2");
	sem_unlink("clientDone");
	sem_unlink("serverDone");
	sem_unlink("done_flag");

	//To set client pid
	if((getClient1 = sem_open("getClient1", O_CREAT,0777,0))==NULL){

		perror("Sem Open Error");
		return 1;
	}
	if((getClient2 = sem_open("getClient2", O_CREAT,0777,0))==NULL){

		perror("Sem Open Error");
		return 1;
	}
	if((init = sem_open("init", O_CREAT, 0777,0))==NULL){
		perror("Sem Open Error");
		return 1;
	}
	if((fromServer2 = sem_open("fromServer2", O_CREAT, 0777,0))==NULL){
		perror("Sem Open Error");
		return 1;
	}

	//To control game flow
	if((writeNum = sem_open("writeNum", O_CREAT,0777,0))==NULL){

		perror("Sem Open Error");
		return 1;
	}
	if((writeSign = sem_open("writeSign", O_CREAT,0777,0))==NULL){

		perror("Sem Open Error");
		return 1;
	}
	if((emptySign = sem_open("emptySign", O_CREAT,0777,0))==NULL){

		perror("Sem Open Error");
		return 1;
	}
	if((emptyNum = sem_open("emptyNum", O_CREAT, 0777,1))==NULL){

		perror("Sem Open Error");
		return 1;
	}
	if((serverDone = sem_open("serverDone", O_CREAT, 0777,0))==NULL){

		perror("Sem Open Error");
		return 1;
	}
	if((clientDone = sem_open("clientDone", O_CREAT, 0777,0))==NULL){

		perror("Sem Open Error");
		return 1;
	}

						
	//Only used in Server.	
	if((canStart = sem_open("canStart", O_CREAT, 0777,0))==NULL){
		perror("Sem Open Error");
		return 1;
	}
	if((toServerClient2 = sem_open("toServerClient2", O_CREAT, 0777,0))==NULL){
		perror("Sem Open Error");
		return 1;
	}

	//Only used in Client
	if((enterClient1=sem_open("enterClient1", O_CREAT, 0777,1))==NULL){
		perror("Sem Open Error");
		return 1;
	}
	if((enterClient2=sem_open("enterClient2", O_CREAT, 0777,0))==NULL){
		perror("Sem Open Error");
		return 1;
	}
	if((storeClient2=sem_open("storeClient2", O_CREAT, 0777,0))==NULL){
		perror("Sem Open Error");
		return 1;
	}
	if((toClient1=sem_open("toClient1", O_CREAT, 0777,0))==NULL){
		perror("Sem Open Error");
		return 1;
	}
	if((forClient1=sem_open("forClient1", O_CREAT, 0777,0))==NULL){
		perror("Sem Open Error");
		return 1;
	}
	if((forClient2=sem_open("forClient2", O_CREAT, 0777,0))==NULL){
		perror("Sem Open Error");
		return 1;
	}
	if((done_flag=sem_open("done_flag", O_CREAT, 0777,0))==NULL){
		perror("Sem Open Error");
		return 1;
	}
	//Shared Memory
	if((fd_input = open(input,O_CREAT|O_RDWR,S_IRUSR|S_IWUSR))==-1){
		perror("Open Error\n");
		return 1;
	}
	if((fd_output = open(output,O_CREAT|O_RDWR,S_IRUSR|S_IWUSR))==-1){
		perror("Open Error\n");
		return 1;
	}
	

	write(fd_input, "", sizeof(int)*2);
	write(fd_output, "", sizeof(int) );
	//indicate shared memory
	ptr_input = mmap(NULL,2*sizeof(int), PROT_READ|PROT_WRITE,MAP_SHARED,fd_input,0);
	ptr_output = mmap(NULL, sizeof(int), PROT_READ|PROT_WRITE,MAP_SHARED,fd_output,0);

	// create thread
	pthread_create(&pt[0], NULL, (void*)&client_1, NULL);
	pthread_create(&pt[1], NULL, (void*)&client_2, NULL);


	ptr_input[0]=0;
	ptr_input[1]=0;
	ptr_output[0]=0;

	pthread_join(pt[0], (void*)&status);
	pthread_join(pt[1], (void*)&status);


	printf("done.\n");
	close(fd_input);
	close(fd_output);

	return 0;
}

void client_1(void){

	sem_wait(getClient1);

	client1_pid = ptr_input[0];
	printf("Client 0 hi\n");
	
	sem_post(toServerClient2);
	sem_wait(canStart);

	while(1){
		sem_wait(writeSign);
	
		switch(ptr_input[0]){
			case 1:printf("[%d]1(+)\n", client1_pid);
				break;
			case 2:	printf("[%d]2(-)\n", client1_pid);
				break;
			case 3: printf("[%d]3(=)\n", client1_pid);
				sem_post(emptyNum);
				sem_post(forClient2);
				return;
			default: ptr_input[0]=0;ptr_input[1]=0;			
		}

		sem_post(emptyNum);
		sem_post(forClient2);
	}

	return;
}
void client_2(void){

	sem_wait(getClient2);

	client2_pid = ptr_input[1];
	printf("Client 1 hi\n");	
	printf("=====================\n\tGame Start!!\n=====================\n");
	
	sem_wait(toServerClient2);
	sem_wait(init);
	ptr_input[0]=0;
	ptr_input[1]=0;

	sem_post(fromServer2);
	sem_post(canStart);
	//game start

	while(1){
		sem_wait(writeNum);
		if(ptr_input[0]!=0||ptr_output[0]!=0){//not first order
			if(ptr_input[1]<=100 && 1<= ptr_input[1]){
				printf("[%u]%d\n",client2_pid, ptr_input[1]);
				if(ptr_input[0]==3){
					if(ptr_input[1]!=*ptr_output){
					/*client_1 win!*/ 
						printf("%u win!!\n", client1_pid);
						sem_post(emptySign);
						return;
					}
					else{
					/*client_2 win!*/
						printf("%u win!!\n", client2_pid);
						sem_post(emptySign);
						return;
					}
				}else if(ptr_input[0]==1){
					ptr_output[0]=ptr_input[1]+ptr_output[0];
				}else if(ptr_input[0]==2){
					ptr_output[0]=ptr_output[0]-ptr_input[1];
				}else if(ptr_input[0]==0 &&ptr_output[0]==0) ptr_output[0]=ptr_input[1];
				else if(ptr_input[0]==0 && ptr_output[0]!=0) ptr_input[1]=0;
			}
			else ptr_input[1]=0;
		}
		else if(ptr_input[0]==0 && ptr_output[0]==0){//first order
			if(ptr_input[1]<=100 && 1<=ptr_input[1]){
			       	printf("[%u]%d\n",client2_pid, ptr_input[1]);
				ptr_output[0]=ptr_input[1];
			}else ptr_input[1]=0;

		}
		sem_post(emptySign);
	}
	return;
}
