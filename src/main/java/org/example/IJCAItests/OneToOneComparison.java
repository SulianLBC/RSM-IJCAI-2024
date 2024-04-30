package org.example.IJCAItests;

import org.chocosolver.solver.exception.ContradictionException;
import org.example.manyToMany.CPInstance;
import org.example.manyToMany.LocalSearch;
import org.example.manyToMany.ReducedLocalSearch;
import org.example.manyToMany.ProcessInstance;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import static org.example.Main.generateLists;

public class OneToOneComparison {
    public static void main(String[] args) throws DirectedAcyclicGraph.CycleFoundException, IOException, ContradictionException
    {   
        // Arguments : number of men (N), number of instances (kfoldValue), time limit (timeLimit)
        long seed = 19900603;
        long timeLimit = Integer.parseInt(args[2]) * 1000;
        String timeLimit_string = args[2] + 's';
        //int cutOffLimit = 1000;
        int kfoldValue = Integer.parseInt(args[1]);
        int[] N = {Integer.parseInt(args[0])};

        // long timeLimit = 60 * 1000;
        // String timeLimit_string = "60s";
        // //int cutOffLimit = 1000;
        // int kfoldValue = 2;
        // int[] N = {500};

        FileWriter pw = new FileWriter(new File("comparison_SM_" + N[0] + "_" + kfoldValue + "_" + timeLimit_string + ".csv"));
        pw.append("N");
        pw.append(',');
        pw.append("PP1");
        pw.append(',');
        pw.append("PP2");
        pw.append(',');
        pw.append("|V|");
        pw.append(',');
        pw.append("|B|");
        pw.append(',');
        pw.append("R_up");
        pw.append(',');
        pw.append("R_down");
        pw.append(',');
        pw.append("Pairs");
        pw.append(',');
        pw.append("Pairs_reduced");
        pw.append(',');
        pw.append("b_ub");
        pw.append(',');
        pw.append("b_LS");
        pw.append(',');
        pw.append("LS_time");
        pw.append(',');
        pw.append("b_LS_bis");
        pw.append(',');
        pw.append("LS_time_bis");
        pw.append(',');
        pw.append("b_CP");
        pw.append(',');
        pw.append("CP_time_find");
        pw.append(',');
        pw.append("CP_time_total");
        pw.append(',');
        pw.append("CP_status");
        pw.append(',');
        pw.append("b_CP_bis");
        pw.append(',');
        pw.append("CP_time_bis_find");
        pw.append(',');
        pw.append("CP_time_bis_total");
        pw.append('\n');

        float averagePP1;
        float averagePP2;
        float averageV;
        float averageB;
        float averageRup;
        float averageRdown;
        float averagePairs;
        float averagePairsReduced;
        float averageBOUNDb;
        float averageLSb;
        float averageLStime;
        float averageLSb_bis;
        float averageLStime_bis;
        float averageCPb;
        float averageCPtimeFind;
        float averageCPtimeTotal;
        float averageStatus;
        float averageCPb_bis;
        float averageCPtimeFind_bis;
        float averageCPtimeTotal_bis;
        long start;

        for (int i = 0; i < N.length; i++) {
            System.out.println("Size : " + N[i]);
            averagePP1 = 0;
            averagePP2 = 0;
            averageV = 0;
            averageB = 0;
            averageRup = 0;
            averageRdown = 0;
            averagePairs = 0;
            averagePairsReduced = 0;
            averageBOUNDb = 0;
            averageLSb = 0;
            averageLStime = 0;
            averageLSb_bis = 0;
            averageLStime_bis = 0;
            averageCPb = 0;
            averageCPtimeFind = 0;
            averageCPtimeTotal = 0;
            averageStatus = 0;
            averageCPb_bis = 0;
            averageCPtimeFind_bis = 0;
            averageCPtimeTotal_bis = 0;

            for (int k = 0; k < kfoldValue; k++) {
                // Generating the random instance
                int[][][] preferences = generateLists(N[i]);
                int[][] malePref = preferences[0];
                int[][] femalePref = preferences[1];
                for (int a = 0; a < malePref.length; a++) {
                    for (int b = 0; b < malePref.length; b++) {
                        malePref[a][b] = malePref[a][b] - 1;
                        femalePref[a][b] = femalePref[a][b] - 1;
                    }
                }
                int[] maleCap = new int[N[i]];
                Arrays.fill(maleCap, 1);
                int[] femaleCap = new int[N[i]];
                Arrays.fill(femaleCap, 1);

                // Preprocessing step 1
                ProcessInstance instance = new ProcessInstance(malePref, femalePref, maleCap, femaleCap);
                start = System.currentTimeMillis();
                instance.runProcedure();
                averagePP1 += (System.currentTimeMillis() - start);
                averageV += instance.metaRotations.size();
                averagePairs += instance.rotationOX.keySet().size();
                
                // LS run
                LocalSearch ls = new LocalSearch(instance);
                ls.generalProcedure(timeLimit, 50, 10000);
                averageLSb += ls.bestValue;
                averageLStime += ls.timeBestSolution;

                // naive CP run
                CPInstance cp = new CPInstance(instance);
                cp.runNaive = true;
                cp.timeLimit = timeLimit_string;
                cp.solve();
                averageCPb += cp.optValue;
                averageCPtimeFind += cp.timeBestSolution;
                averageCPtimeTotal += cp.solveTime;
                averageStatus += cp.solveStatus;

                // reduced CP run + Preprocessing step 2
                cp = new CPInstance(instance);
                cp.runNaive = false;
                cp.solve();
                averageBOUNDb += cp.OptUB + 1;
                averageCPb_bis += cp.optValue;
                averageCPtimeFind_bis += cp.timeBestSolution;
                averageCPtimeTotal_bis += cp.solveTime;
                averagePP2 += cp.preProcessTime;
                averageB += cp.SuperstableUB.length - cp.SuperstableLB.length;
                averagePairsReduced += cp.reducedRotationOX.keySet().size();
                averageRup += cp.reducedRotation_up.length;
                averageRdown += cp.reducedRotation_down.length;

                // LS reduced run
                ReducedLocalSearch rls = new ReducedLocalSearch(instance, cp);
                rls.generalProcedure(timeLimit, 50, 10000);
                averageLSb_bis += rls.bestValue;
                averageLStime_bis += rls.timeBestSolution;
            }

            pw.append("" + N[i]);
            pw.append(',');
            pw.append("" + averagePP1 / (kfoldValue * 1000));
            pw.append(',');
            pw.append("" + averagePP2 / kfoldValue);
            pw.append(',');
            pw.append("" + averageV / kfoldValue);
            pw.append(',');
            pw.append("" + averageB / kfoldValue);
            pw.append(',');
            pw.append("" + averageRup / kfoldValue);
            pw.append(',');
            pw.append("" + averageRdown/ kfoldValue);
            pw.append(',');
            pw.append("" + averagePairs / kfoldValue);
            pw.append(',');
            pw.append("" + averagePairsReduced / kfoldValue);
            pw.append(',');
            pw.append("" + averageBOUNDb / kfoldValue);
            pw.append(',');
            pw.append("" + averageLSb / kfoldValue);
            pw.append(',');
            pw.append("" + averageLStime / (kfoldValue * 1000));
            pw.append(',');
            pw.append("" + averageLSb_bis / kfoldValue);
            pw.append(',');
            pw.append("" + averageLStime_bis / (kfoldValue * 1000));
            pw.append(',');
            pw.append("" + averageCPb / kfoldValue);
            pw.append(',');
            pw.append("" + averageCPtimeFind / kfoldValue);
            pw.append(',');
            pw.append("" + averageCPtimeTotal / kfoldValue);
            pw.append(',');
            if (averageStatus > 0) {
                pw.append("TimeOut");
            } else {
                pw.append("Optimal");
            }
            pw.append(',');
            pw.append("" + averageCPb_bis / kfoldValue);
            pw.append(',');
            pw.append("" + averageCPtimeFind_bis / kfoldValue);
            pw.append(',');
            pw.append("" + averageCPtimeTotal_bis / kfoldValue);
            pw.append('\n');

            pw.flush();
            System.gc();
        }
        pw.close();
    }
}
