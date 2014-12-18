# blackout

## Pre-requisites

1. Install Riemann and associated tools

When using Mac OS X, issue the following commands at a terminal: 

a. `brew install riemann`

b. `sudo gem install riemann-client riemann-tools riemann-dash`

c. `riemann /usr/local/etc/riemann.config` 

d. `sudo riemann-dash` (sudo is necessary to permit writes for config saves)

e. Open your web browser to http://localhost:4567

## Copyright and License

Copyright © 2014 Vital Labs, Inc.

Hello,

There’s always been an implicit assumption that operands in a expression are 
evaluated from left to right. But in Clojure and other functional programming 
languages, most values we deal with are immutable and most functions do not 
depend on side effects; those that do are the exception. 

I propose a hybrid Lisp evaluator and JIT compiler that drops this assumption 
for all forms excepting the `do` special form, which will always evaluate its 
arguments sequentially. Since let and lambda expression bodies expand into 
implicit `do` forms, the implicit contract assumed in these contexts will be 
maintained so as to not introduce unexpected effects for the programmer. The 
evaluator will use a special purpose thread pooling algorithm that is 
automatically tuned for the computer it is being executed on based on its 
capabilities. It will pool threads for both asynchronous and synchronous tasks 
on this singleton pool. This design eliminates the need for user programs to 
make these sorts of decisions or even consider them at all. Advanced programmers
can of course augment the compiler by using special forms that affect the 
runtime semantics of the expressions; this is similar to how `let` will always 
perform sequential binding and of course the aforementioned `do`. Outside of 
these special forms, all arguments to a procedure will always be evaluated in 
parallel, up to the maximum concurrency level as determined upon system 
initialization. 

It has been well documented in the literature that one of the greatest 
computational costs involved with multithreaded programs is the time it takes 
for one CPU to perform a "context switch", that is, to stop what it is doing, 
wait for OS to coordinate it, and resume execution on another thread. Therefore
the answer to any problem is never to "throw more threads" at it, because this
just adds the overall contention for CPU time each thread would need to complete
its task. Therefore it is absolutely essential that an efficient program limit
the number of threads it makes use of. Furthermore, creating and destroying 
threads is an expensive operation by itself; many naively multithreaded 
programs often spend more time doing thread initialization and cleanup than 
computation. A parallel Lisp evaluator solves this problem by using a fixed 
thread pool whose size is determined by hardware and OS which is executing it.

It is has been observed that it is more efficient to use a fixed 
number of threads and pool their use locally in your program than it is to 
create new threads every time another is needed. But this is a complex task and
 very few thread pools are useful in contexts they were not designed to be 
efficient in. So what often happens is you end up having to create new 
specialized pools for every sufficiently complex problem that you come across.

I hypothesize this is because programs written in most languages do not have a 
well defined execution model that is related to the syntax of the programming 
language which makes it impossible to deterministically augment the execution 
of a program at runtime through syntax manipulation. However, in Lisp this is 
not true. The evaluation of expressions in Lisp is a well defined procedure, 
mathematical in its origins, derived directly from the `Mechanical Evaluation 
of Expressions` (see relevant paper by Peter J. Landin for more information). 

Because the thread pool is managed by the evaluator, no user program should 
ever have to think about interfacing with it directly and instead should focus 
on the semantics of the runtime. The advanced programmer will be challenged to 
figure out how to manipulate their program so that the evaluator will achieve 
the desired efficiency level. Metaprogramming becomes the key to optimization.

Since Lisp s-expressions are programmable at a stage before evaluation takes 
place, a wide array of optimization opportunities are available through user 
defined macros. This opens the door to shifting the complexity burden of 
multithreaded programming from runtime manipulation of threads and system 
specific semantics to compile time metaprogramming and the semantics of the 
language. When extensive optimization in a multithreaded scenario is warranted,
the programmer would be tasked not with handling threads, but with writing a procedure that rewrites their programs in a way that is amenable to efficient execution by the evaluator to maximize effective parallelism. I argue that this 
complexity shift would be a net gain because Lisp programmers are already well 
aware of the benefits and power of metaprogramming, as well as the complexity 
tradeoffs that come with them.

Adrian
