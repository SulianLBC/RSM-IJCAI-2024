# ROBUST STABLE MATCHING

The code for paper "Polynomial Time Presolve Algorithms for Rotation-Based Models Solving the Robust Stable Matching Problem" submitted to the Main Track of IJCAI 2024.


# PROJECT ARCHITECTURE

## Version

This project is implemented in Java 21 and uses the 21.0.1 version of openjdk.

This project uses Maven to manage the dependencies and call the packages choco-solver and jgraph.

## Main.java file

The Main.java file contains two static methods in order to generate the random instances.

## manyToMany package

The manyToMany package contains all the models and algorithms related to the Robust Stable Matching Problem (RSM) generalised to the many-to-many instances.

- ProcessInstance.java : This file contains the algorithms that allow to get M_0, M_Z and the directed graph of rotations. This corresponds to the preprocessing step 1 (PP1) as explained in the submitted paper. The algorithms come from the three papers cited in Section 4.1.

- CPInstance.java : This file contains the presolve algorithms presented in the submitted papers and corresponding to PP2, which are Algorithm 1 (reduceVariables() method), Algorithm 2 (reduceDomains() method) and the algorithms to get the upper bound (computeMedianUpDown(), computeMO() and computeMZ() methods). It also contains some intermediary methods to compute the robust score or update data structures.

- CPModel.java : This file contains the CP model solving the RSM problem for many-to-many instances. The first model (run_naive_model() method) does not take into account the results of the presolve algorithms of PP2. The second model (run_model_with_reduced_variables() method) does exploit PP2.

- LocalSearch.java : This file contains the local search model LS presented in the submitted paper and adapted to the many-to-many instances.

- ReducedLocalSearch.java : This file contains the LS model that can exploit PP2.

## propagators package

The propagator package contains the propagators of constraints specifically developped for the RSM problem and that are not initially implemented in choco-solver.

Particularly, the constraints ClosedSubSet and IndexUnion detailed in the Technical Appendix are implemented in this package.

## IJCAItests package

The IJCAItests package contains the scripts that allow to get the experimental results presented in the submitted paper and the Technical Appendix.

## results folder

The results folder contains the files obtained by running the scripts in IJCAItests package. Those are the results presented in the submitted paper and the Technical Appendix.

# HOW TO USE THIS PROJECT

In order to run a specific model on a specific instance, a user can generate their own instance and run it on the model of their choice.

To run the benchmarks, one can use the jar file of the project RSM.jar and call it in command line.
The three following section explain how to run a benchmarck on some instances between the models LS and CP with and without PP2.


## Experiments on SM instances

To get a benchmark on SM instances, one can run the OneToOneComparison.java script with the following three arguments : 
- Instance size : an integer corresponding to the number of men (and women).
- Number of instances : an integer corresponding to the number of random instances that will be generated and for which the mean statistics will be reported.
- Time limit : an integer corresponding to the time limit (in seconds) for the models LS and CP without PP2.


Here is an exemple : 

java -cp .\RSM.jar org.example.IJCAItests.OneToOneComparison 1000 10 60

A result file will be created in the current directory.


## Experiments on HR instances

To get a benchmark on HR instances, one can run the OneToManyComparison.java script with the following five arguments : 
- Number of men : an integer corresponding to the number of men.
- Number of women : an integer corresponding to the number of women.
- Capacity : an integer corresponding to the capacity of each women.
- Number of instances : an integer corresponding to the number of random instances that will be generated and for which the mean statistics will be reported.
- Time limit : an integer corresponding to the time limit (in seconds) for the models LS and CP without PP2.


Here is an exemple : 

java -cp .\RSM.jar org.example.IJCAItests.OneToManyComparison 280 40 7 10 60

A result file will be created in the current directory.

## Experiments on MM instances

To get a benchmark on MM instances, one can run the ManyToManyComparison.java script with the following four arguments : 
- Number of men/women : an integer corresponding to the number of men (and women).
- Capacity : an integer corresponding to the capacity of each individual.
- Number of instances : an integer corresponding to the number of random instances that will be generated and for which the mean statistics will be reported.
- Time limit : an integer corresponding to the time limit (in seconds) for the models LS and CP without PP2.


Here is an exemple : 

java -cp .\RSM.jar org.example.IJCAItests.ManyToManyComparison 100 3 10 60

A result file will be created in the current directory.



