package org.example.propagators;

import java.util.Arrays;

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.SetVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.solver.variables.delta.ISetDeltaMonitor;
import org.chocosolver.solver.variables.events.PropagatorEventType;
import org.chocosolver.solver.variables.events.SetEventType;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.graphs.DirectedGraph;
import org.chocosolver.util.objects.setDataStructures.ISetIterator;
import org.chocosolver.util.procedure.IntProcedure;

public class PropClosedSubSet extends Propagator<SetVar> {
    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

    private final SetVar subSet;
    private final DirectedGraph poSet;
    private final boolean notEmpty;
    private final ISetDeltaMonitor sdm;
    private final IntProcedure subSetForced;
    private final IntProcedure subSetRemoved;

    //***********************************************************************************
    // CONSTRUCTORS
    //***********************************************************************************

    /**
     * Assures subSet is a closed subset according to poSet
     *
     * @param subSet   set variable
     * @param poSet    the graph representing the partial ordered set
     * @param notEmpty true : the set variable cannot be empty
     *                 false : the set may be empty
     */
    public PropClosedSubSet(SetVar subSet, DirectedGraph poSet, boolean notEmpty) {
        super(new SetVar[]{subSet}, PropagatorPriority.BINARY, true);
        this.subSet = (SetVar) subSet;
        this.poSet = poSet;
        this.notEmpty = notEmpty;
        this.sdm = this.subSet.monitorDelta(this);

        // PROCEDURES
        this.subSetForced = element -> {
            for (int u : poSet.getPredecessorsOf(element)) {
                subSet.force(u, this);
            }
        };
        this.subSetRemoved = element -> {
            for (int v : poSet.getSuccessorsOf(element)) {
                subSet.remove(v, this);
            }
        };
    }

    //***********************************************************************************
    // METHODS
    //***********************************************************************************

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        if (PropagatorEventType.isFullPropagation(evtmask)) {
            ISetIterator iter = subSet.getLB().iterator();
            while (iter.hasNext()) {
                int v = iter.nextInt();
                for (int u : poSet.getPredecessorsOf(v)) {
                    if (!(subSet.getLB().contains(u))) {
                        subSet.force(u, this);
                    }
                }
            }

            iter = subSet.getUB().iterator();
            while (iter.hasNext()) {
                int v = iter.nextInt();
                for (int u : poSet.getPredecessorsOf(v)) {
                    if (subSet.getUB().contains(v) && !(subSet.getUB().contains(u))) {
                        subSet.remove(v, this);
                        break;
                    }
                }
            }
            sdm.startMonitoring();
        }
    }

    @Override
    public void propagate(int idxVarInProp, int mask) throws ContradictionException {
        if (idxVarInProp == 0) {
            sdm.forEach(subSetForced, SetEventType.ADD_TO_KER);
            sdm.forEach(subSetRemoved, SetEventType.REMOVE_FROM_ENVELOPE);
        }
    }

    @Override
    public ESat isEntailed() {
        if (subSet.getUB().size() == 0) {
            if (notEmpty) {
                return ESat.FALSE;
            } else {
                return ESat.TRUE;
            }
        }

        ISetIterator iter = subSet.getLB().iterator();
        while (iter.hasNext()) {
            int v = iter.nextInt();
            for (int u : poSet.getPredecessorsOf(v)) {
                if (!(subSet.getUB().contains(u))) {
                    return ESat.FALSE;
                }
            }
        }

        if (isCompletelyInstantiated()) {
            return ESat.TRUE;
        }
        return ESat.UNDEFINED;
    }
}