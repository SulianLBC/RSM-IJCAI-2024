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

public class PropSetDifference extends Propagator<SetVar> {
    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

    private final SetVar Z;
    private final SetVar A;
    private final SetVar B;
    private final boolean notEmpty;
    private final ISetDeltaMonitor[] sdm;
    private final IntProcedure ZForced;
    private final IntProcedure ZRemoved;
    private final IntProcedure AForced;
    private final IntProcedure ARemoved;
    private final IntProcedure BForced;
    private final IntProcedure BRemoved;

    //***********************************************************************************
    // CONSTRUCTORS
    //***********************************************************************************

    /**
     * Z = A \ B, where Z, A and B are sets
     *
     * @param setZ  set variable
     * @param setA   set variable
     * @param setB   set variable
     * @param notEmpty true : the set variables cannot be empty
     *                 false : the set variables may be empty
     */
    public PropSetDifference(SetVar setZ, SetVar setA, SetVar setB, boolean notEmpty) {
        super(new SetVar[]{setZ, setA, setB}, PropagatorPriority.BINARY, true);
        this.Z = (SetVar) setZ;
        this.A = (SetVar) setA;
        this.B = (SetVar) setB;
        this.notEmpty = notEmpty;
        this.sdm = new ISetDeltaMonitor[]{this.Z.monitorDelta(this), this.A.monitorDelta(this), this.B.monitorDelta(this)};

        // PROCEDURES
        this.ZForced = element -> {
            A.force(element, this);
            B.remove(element, this);
        };
        this.ZRemoved = element -> {
            if (A.getLB().contains(element)) {
                B.force(element, this);
            } else if (!(B.getUB().contains(element))) {
                A.remove(element,this);
            }
        };
        this.AForced = element -> {
            if (!(B.getUB().contains(element))) {
                Z.force(element, this);
            } else if (!(Z.getUB().contains(element))) {
                B.force(element, this);
            }
        };
        this.ARemoved = element -> {
            Z.remove(element,this);
        };
        this.BForced = element -> {
            Z.remove(element, this);
        };
        this.BRemoved = element -> {
            if (A.getLB().contains(element)) {
                Z.force(element, this);
            } else if (Z.getLB().contains(element)) {
                A.force(element,this);
            } else if (!(A.getUB().contains(element))) {
                Z.remove(element,this);
            } else if (!(Z.getUB().contains(element))) {
                A.remove(element,this);
            }
        };
    }

    //***********************************************************************************
    // METHODS
    //***********************************************************************************

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        if (PropagatorEventType.isFullPropagation(evtmask)) {
            ISetIterator iter = Z.getUB().iterator();
            while (iter.hasNext()) {
                int e = iter.nextInt();
                if (!(A.getUB().contains(e)) || B.getLB().contains(e)) {
                    Z.remove(e, this);
                } else if (A.getLB().contains(e) && !(B.getUB().contains(e))) {
                    Z.force(e, this);
                }
            }

            iter = A.getUB().iterator();
            while (iter.hasNext()) {
                int e = iter.nextInt();
                if (Z.getLB().contains(e)) {
                    A.force(e, this);
                } else if (!(Z.getUB().contains(e)) && !(B.getUB().contains(e))) {
                    A.remove(e, this);
                }
            }

            iter = B.getUB().iterator();
            while (iter.hasNext()) {
                int e = iter.nextInt();
                if (Z.getLB().contains(e)) {
                    B.remove(e, this);
                } else if (!(Z.getUB().contains(e)) && A.getLB().contains(e)) {
                    B.force(e, this);
                }
            }
            sdm[0].startMonitoring();
            sdm[1].startMonitoring();
            sdm[2].startMonitoring();
        }

    }

    @Override
    public void propagate(int idxVarInProp, int mask) throws ContradictionException {
        if (idxVarInProp ==  0) {
            sdm[idxVarInProp].forEach(ZForced, SetEventType.ADD_TO_KER);
            sdm[idxVarInProp].forEach(ZRemoved, SetEventType.REMOVE_FROM_ENVELOPE);
        } else if (idxVarInProp ==  1) {
            sdm[idxVarInProp].forEach(AForced, SetEventType.ADD_TO_KER);
            sdm[idxVarInProp].forEach(ARemoved, SetEventType.REMOVE_FROM_ENVELOPE);
        } else if (idxVarInProp ==  2) {
            sdm[idxVarInProp].forEach(BForced, SetEventType.ADD_TO_KER);
            sdm[idxVarInProp].forEach(BRemoved, SetEventType.REMOVE_FROM_ENVELOPE);
        }
    }

    @Override
    public ESat isEntailed() {

        if (Z.getUB().size() == 0 && A.getUB().size() == 0 && B.getUB().size() == 0) {
            if (notEmpty) {
                return ESat.FALSE;
            } else {
                return ESat.TRUE;
            }
        }

        ISetIterator iter = Z.getLB().iterator();
        while (iter.hasNext()) {
            int e = iter.nextInt();
            if (!(A.getUB().contains(e)) || B.getLB().contains(e)) {
                return ESat.FALSE;
            }
        }

        iter = A.getLB().iterator();
        while (iter.hasNext()) {
            int e = iter.nextInt();
            if (!(Z.getUB().contains(e)) && !(B.getUB().contains(e))) {
                return ESat.FALSE;
            }
        }

        iter = B.getLB().iterator();
        while (iter.hasNext()) {
            int e = iter.nextInt();
            if (Z.getLB().contains(e)) {
                return ESat.FALSE;
            }
        }

        if (isCompletelyInstantiated()) {
            return ESat.TRUE;
        }

        return ESat.UNDEFINED;
    }
}