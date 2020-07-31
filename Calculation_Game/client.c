#include <stdio.h>
#include <semaphore.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/mman.h>

const char* input = "input.db";
const char* output = "output.db";

int main(void){

	int sign;
	int number;
	int fd_input;
	int fd_output;
	int* ptr_input;
	int* ptr_output;

	sem_t* forClient1;
	sem_t* forClient2;
	sem_t* enterClient1;
	sem_t* enterClient2;
	sem_t* getClient1;
	sem_t* getClient2;
	sem_t* storeClient2;
	sem_t* init;
	sem_t* toClient1;
	sem_t* fromServer2;
	sem_t* serverDone;
	sem_t* clientDone;
	sem_t* done_flag;

	sem_t* writeNum;
	sem_t* writeSign;
	sem_t* emptyNum;
	sem_t* emptySign;


	int other_pid=0;
	int done_flag_client=0;

	//Shared semaphore with Server
	if((getClient1=sem_open("getClient1", O_EXCL, 0777,0))==NULL){
		perror("Sem Open Error");
		return 1;
	}
	if((getClient2 = sem_open("getClient2",O_EXCL, 0777,0))==NULL){
		perror("Sem Open Error");
		return 1;
	}
	
	if((init = sem_open("init",O_EXCL, 0777,0))==NULL){
		perror("Sem Open Error");
		return 1;
	}
	if((fromServer2 = sem_open("fromServer2",O_EXCL, 0777,0))==NULL){
		perror("Sem Open Error");
		return 1;
	}
	
	//control 4 process/thread running order between server and client
	if((writeNum=sem_open("writeNum", O_EXCL, 0777,0))==NULL){
		perror("Sem Open Error");
		return 1;
	}
	if((writeSign=sem_open("writeSign", O_EXCL, 0777,0))==NULL){
		perror("Sem Open Error");
		return 1;
	}
	if((emptySign=sem_open("emptySign", O_EXCL, 0777,0))==NULL){
		perror("Sem Open Error");
		return 1;
	}
	if((emptyNum=sem_open("emptyNum", O_EXCL, 0777,1))==NULL){
		perror("Sem Open Error");
		return 1;
	}
	//only used in client.c
	if((enterClient1=sem_open("enterClient1", O_EXCL, 0777,1))==NULL){
		perror("Sem Open Error");
		return 1;
	}
	if((enterClient2=sem_open("enterClient2", O_EXCL, 0777,0))==NULL){
		perror("Sem Open Error");
		return 1;
	}
	if((storeClient2=sem_open("storeClient2", O_EXCL, 0777,0))==NULL){
		perror("Sem Open Error");
		return 1;
	}
	if((toClient1=sem_open("toClient1", O_EXCL, 0777,0))==NULL){
		perror("Sem Open Error");
		return 1;
	}
	if((forClient1=sem_open("forClient1", O_EXCL, 0777,0))==NULL){
		perror("Sem Open Error");
		return 1;
	}
	if((forClient2=sem_open("forClient2", O_EXCL, 0777,0))==NULL){
		perror("Sem Open Error");
		return 1;
	}
	if((clientDone=sem_open("clientDone",O_EXCL,0777,0))==NULL){
		perror("Sem Open Error");
		return 1;
	}
	if((serverDone=sem_open("serverDone",O_EXCL,0777,0))==NULL){
		perror("Sem Open Error");
		return 1;
	}
	if((done_flag=sem_open("done_flag",O_EXCL,0777,0))==NULL){
		perror("Sem Open Error");
		return 1;
	}

	
	

	//Shared Memory
	if((fd_input = open(input,O_CREAT|O_RDWR, S_IRUSR|S_IWUSR))==-1){
		perror("Open Error\n");
		return 1;
	}
	if((fd_output = open(output,O_CREAT|O_RDWR, S_IRUSR|S_IWUSR))==-1){
		perror("Open Error\n");
		return 1;
	}


	ptr_input = mmap(NULL, 2*sizeof(int), PROT_READ | PROT_WRITE, MAP_SHARED, fd_input,0);
	ptr_output = mmap(NULL, sizeof(int), PROT_READ | PROT_WRITE, MAP_SHARED, fd_output,0);	

	


	if(ptr_input[0]==0){//client1 
	
		printf("=========================\n\tHello %u\n\tGame Start!!\n==========================\n",getpid());
		sem_wait(enterClient1);//wait client1	
		ptr_input[0] = (int)getpid(); //input client 1 pid
		sem_post(getClient1);//server get client 1 pid
		sem_post(enterClient2);//after get client 1 pid, get client 2 pid 
		
		sem_wait(storeClient2);//wait client 2
		other_pid =ptr_input[1];//get client 2 pid
		
		sem_post(init);//can init

		sem_wait(toClient1);//after intit
		while(1){
			sem_wait(emptySign);//after client2 write num, server client_2 check num
		
			if(ptr_input[0]!=0){//not null operation
			       //	printf("[%d]%d \n",other_pid,ptr_input[1]);
				
				if(ptr_input[0]==3){
					if(ptr_output[0]==ptr_input[1]){
						printf("[%d]%d \n",other_pid,ptr_input[1]);
						printf("Lose in a Game.\n");
						sem_post(done_flag);
						return 0;
					}else{
						printf("[%d]%d \n",other_pid,ptr_input[1]);
						printf("Win in a Game.\n");
						sem_post(done_flag);
						return 0;
					}
				}
				printf("[%d]%d \n",other_pid,ptr_input[1]);
			}
			else if(ptr_input[0]==0)printf("[%d]%d\n",other_pid, ptr_input[1]);
			

			printf("input +, -, =(+ : 1 / - : 2 / = : 3) : ");
			scanf("%d", &sign);
			ptr_input[0]=sign;
			switch(sign){
				case 1 : printf("[%d]%d(+) \n",getpid(), sign); break;
				case 2 : printf("[%d]%d(-) \n",getpid(), sign); break;
				case 3 : printf("[%d]%d(=) \n",getpid(), sign); break;
			}
						
			sem_post(writeSign);//sign cell is written.
			
		}
	}
	else if(ptr_input[0]!=0 && ptr_input[1]==0){ // client 2
		
		printf("=========================\n\tHello %u\n\tGame Start!!\n==========================\n",getpid());
		sem_wait(enterClient2);//after enter client 1 , can enter client 2
		
		ptr_input[1]= (int)getpid();//input client 2 pid
		other_pid = ptr_input[0];//get client 1 pid
	
		sem_post(getClient2);//server can get client 2 pid

		sem_post(storeClient2);//client 1 get client 2 pid

		sem_wait(fromServer2);//server get clients pid
		sem_post(toClient1);//client 1 execute
		
		while(1){
			
			sem_wait(emptyNum);//start, first order
			
			printf("input num(1~100) : ");
			scanf("%d",&number);
			if(ptr_output[0]==0) { //output=0, 
				ptr_input[1]=number;
				printf("[%d]%d\n",getpid(),number);
				if(ptr_input[0]==3){
                                        if(ptr_output[0]==number){
                                                printf("Win in a Game.\n");
                                                done_flag_client=1;
                                        }else{
                                                printf("Lose in a Game.\n");
                                                done_flag_client=1;
                                        }

                                }

			}else {	
				ptr_input[1] = number;
				printf("[%d]%d ",getpid(),number);
				if(ptr_input[0]==3){
					if(ptr_output[0]==number){
						printf("Win in a Game.\n");
						done_flag_client=1;	
					}else{
						printf("Lose in a Game.\n");
						done_flag_client=1;
					}
					
				}
			}			

			if(done_flag_client==1) {
				sem_post(writeNum);//to server
				sem_wait(done_flag);//wait client 1
				close(fd_input);
				close(fd_output);	
				return 0;
			}
			sem_post(writeNum);//to server
			sem_wait(forClient2);// get client 1 's operate
			switch(ptr_input[0]){
				case 1: printf("[%d]+ \n",other_pid);break;
				case 2: printf("[%d]- \n",other_pid);break;
				case 3: printf("[%d]= \n",other_pid);break;
			}
		

		}


	}
	else if(other_pid!=0){
		printf("You can't enter this room!!\n");
	}
	else {
		
		printf("Do not Server\n");
	}
	
	close(fd_input);
	close(fd_output);
	
	return 0;
}

		
