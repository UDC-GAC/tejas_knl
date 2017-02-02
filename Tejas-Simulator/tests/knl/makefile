FLAGS=-g -O2
NAME=foo

all: foo

foo: foo.cc
	g++ foo.cc -o foo $(FLAGS) -DUNIX --static -DN=$(NN)

multi: multi.cc
	g++ multi.cc -o multi $(FLAGS) -DUNIX --static -lpthread

multi2: multi2.c
	gcc multi2.c -o $(OUT) $(FLAGS) -lpthread -DNUM_THREADS=$(NT)

norm: multi2-noknl.c
	gcc multi2-noknl.c -o norm $(FLAGS) -lpthread -DNUM_THREADS=$(NT)