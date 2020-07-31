#include <stdio.h>
#include <stdlib.h>
#include <wiringPi.h>
#include <pthread.h>
#include <semaphore.h>

#define LED_R	0
#define LED_Y	2
#define LED_G	3
#define SW_W	23
#define SW_R	21
#define SW_Y	22
#define SW_G	24

int ending=0;
int level = 5;
int number=0;
int sequence[10];
int user[10];

sem_t ready_R;
sem_t ready_Y;
sem_t ready_G;
sem_t ready_W;
sem_t next;
sem_t num;

void start_game();
void listen_R();
void listen_Y();
void listen_G();
void listen_end();
void lose();
void win();

int main(){

    int i=0;
    int status;
    pthread_t pt[5];
    if(wiringPiSetup() ==-1){
	exit(1);
    }
    sem_init(&ready_R,0,0);
    sem_init(&ready_Y,0,0);
    sem_init(&ready_G,0,0);
    sem_init(&ready_W,0,0);
    sem_init(&next,0,0);
    sem_init(&num,0,1);

    pthread_create(&pt[0],NULL,(void*)start_game,NULL);
    pthread_create(&pt[1],NULL,(void*)listen_R, NULL);
    pthread_create(&pt[2],NULL,(void*)listen_Y, NULL);
    pthread_create(&pt[3],NULL,(void*)listen_G, NULL);
    pthread_create(&pt[4],NULL,(void*)listen_end, NULL);

    pinMode(SW_R, INPUT);
    pinMode(SW_Y, INPUT);
    pinMode(SW_G, INPUT);
    pinMode(SW_W, INPUT);
    pinMode(LED_R,OUTPUT);
    pinMode(LED_Y, OUTPUT);
    pinMode(LED_G, OUTPUT);
    digitalWrite(LED_R,0);
    digitalWrite(LED_Y,0);
    digitalWrite(LED_G,0);

    pthread_join(pt[0],(void*)&status);
    pthread_join(pt[1],(void*)&status);
    pthread_join(pt[2],(void*)&status);
    pthread_join(pt[3],(void*)&status);
    pthread_join(pt[4],(void*)&status);

    return 0;
}
void start_game(){
    //LED
    int i=0;
    int randNum=0;
    digitalWrite(LED_R,0);
    digitalWrite(LED_Y,0);
    digitalWrite(LED_G,0);

    while(i<3){

	digitalWrite(LED_R,1);
	delay(250);
	digitalWrite(LED_R,0);
	
	digitalWrite(LED_Y,1);
	delay(250);
	digitalWrite(LED_Y,0);

	digitalWrite(LED_G,1);
	delay(250);
	digitalWrite(LED_G,0);
    	i++;
	delay(250);
    }


    //make random array
    while(level < 10){
	number=0;

	printf("right answer\n");
	 srand((unsigned)time(NULL));
   	 for(i=0;i<level;i++){
	     randNum= 1+rand()%3;
	     printf("%d ", randNum);
	     if(randNum ==1) sequence[i]=LED_R;
	     else if(randNum==2) sequence[i]=LED_Y;
	     else sequence[i]=LED_G;
   	 }
	 printf("\n");
   	 //show array
   	 for(i=0;i<level;i++){
		
	     digitalWrite(sequence[i],1);
	     delay(250);
	     digitalWrite(sequence[i],0);
	     delay(250);
	 }
	 printf("user input\n");
    	sem_post(&ready_R);
        sem_post(&ready_Y);
   	sem_post(&ready_G);
 	sem_post(&ready_W);
	
	sem_wait(&next);
	if(ending == 1){
	    sem_post(&ready_R);
	    sem_post(&ready_Y);
	    sem_post(&ready_G);
	}
    }

}
void listen_R(){
    
    while(level<10){
	sem_wait(&ready_R);
	while(1){
	    if(digitalRead(SW_R) ==0 && ending==0){
		while(digitalRead(SW_R)==0){
		    digitalWrite(LED_R,1);
		}
		printf("red\n");

		sem_wait(&num);
		if(number < level){
		    user[number]=LED_R;
		}
		number++;
		sem_post(&num);
		
		digitalWrite(LED_R,1);
	    }
	    digitalWrite(LED_R,0);
	    if(ending==1) break;
    	}
    }
}
void listen_Y(){

    while(level<10){
	sem_wait(&ready_Y);
	while(1){
	    if(digitalRead(SW_Y) ==0 && ending==0){
		while(digitalRead(SW_Y)==0){
		    digitalWrite(LED_Y,1);
		}
		printf("yellow\n");

		sem_wait(&num);
		if(number < level){
		    user[number]=LED_Y;
		}
		number++;
		sem_post(&num);
		digitalWrite(LED_Y,1);
	    }
	    digitalWrite(LED_Y,0);
	    if(ending==1) break;
    	}
    }
}

void listen_G(){
    int iter =0;
    while(level<10){
	sem_wait(&ready_G);
	while(1){
	    if(digitalRead(SW_G) ==0 && ending==0){
		while(digitalRead(SW_G)==0){
		    digitalWrite(LED_G,1);
		}
		
		sem_wait(&num);
		if(number < level){
		    user[number]=LED_G;
		}
		number++;
		sem_post(&num);
		
		printf("green\n");
		digitalWrite(LED_G,1);
	    }
	    digitalWrite(LED_G,0);
	    if(ending==1) break;
    	}
    }
}

void listen_end(){
    int i=0;
    int lose_flag=0;

    while(level<10){
	sem_wait(&ready_W);
	while(1){
		if(digitalRead(SW_W)==0){
		   while(digitalRead(SW_W)==0){}
		    printf("white\n");
		    printf("input terminated\n");
	
		    ending=1;
		    //check input		
		    if(level!=number){
			lose_flag=1;
			printf("you lose!\n");
			
		    }else{
			for(i=0;i<level;i++){
			    if(sequence[i]!=user[i]) {
				printf("you lose\n");
				lose_flag=1;
				break;
			    }
			}
		    }
		    if(lose_flag==1) {
			lose();
			level=10;
			ending=1;
		    }else {
			level++;
			if(level==10) {
			    ending=1;
			    win();
			    printf("you win!\n");
			}
			else {
			    ending=0;
			    printf("next level\n");
			}
		    }
		    sem_post(&next);
		    break;
		}
	    }
    }
   
}
void lose(){
    int i=0;
    while(i<3){

	digitalWrite(LED_R,1);
	digitalWrite(LED_Y,1);
	digitalWrite(LED_G,1);
	delay(250);
	digitalWrite(LED_R,0);
	digitalWrite(LED_Y,0);
	digitalWrite(LED_G,0);
    	delay(250);
	i++;
    }
}
void win(){
    int i=0;
    while(i<3){
	digitalWrite(LED_R,1);
	delay(250);
	digitalWrite(LED_R,0);
	
	digitalWrite(LED_Y,1);
	delay(250);
	digitalWrite(LED_Y,0);

	digitalWrite(LED_G,1);
	delay(250);
	digitalWrite(LED_G,0);
    	i++;
	delay(250);

    }
}
