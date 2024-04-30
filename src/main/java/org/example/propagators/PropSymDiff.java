package org.example.propagators;

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.SetVar;
import org.chocosolver.solver.variables.delta.ISetDeltaMonitor;
import org.chocosolver.solver.variables.events.PropagatorEventType;
import org.chocosolver.solver.variables.events.SetEventType;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.setDataStructures.ISetIterator;
import org.chocosolver.util.procedure.IntProcedure;

public class PropSymDiff extends Propagator<SetVar> {
    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

    private final SetVar A;
    private final SetVar B;
    private final SetVar C;
    private final boolean notEmpty;
    private final ISetDeltaMonitor[] sdm;
    private final IntProcedure AForced;
    private final IntProcedure ARemoved;
    private final IntProcedure BForced;
    private final IntProcedure BRemoved;
    private final IntProcedure CForced;
    private final IntProcedure CRemoved;

    //***********************************************************************************
    // CONSTRUCTORS
    //***********************************************************************************

    /**
     * Each set variable is the symetric difference between the two others
     *
     * @param setA   set variable
     * @param setB   set variable
     * @param setC   set variable
     * @param notEmpty true : the set variables cannot be empty
     *                 false : the set variables may be empty
     */
    public PropSymDiff(SetVar setA, SetVar setB, SetVar setC, boolean notEmpty) {
        super(new SetVar[]{setA, setB, setC}, PropagatorPriority.BINARY, true);
        this.A = (SetVar) setA;
        this.B = (SetVar) setB;
        this.C = (SetVar) setC;
        this.notEmpty = notEmpty;
        this.sdm = new ISetDeltaMonitor[]{this.A.monitorDelta(this), this.B.monitorDelta(this), this.C.monitorDelta(this)};

        // PROCEDURES
        this.AForced = element -> {
            if (!(B.getUB().contains(element))) {
                C.force(element, this);
            } else if (!(C.getUB().contains(element))) {
                B.force(element, this);
            } else if (B.getLB().contains(element)) {
                C.remove(element, this);
            } else if (C.getLB().contains(element)) {
                B.remove(element,this);
            }
        };
        this.ARemoved = element -> {
            if (!(B.getUB().contains(element))) {
                C.remove(element, this);
            } else if (!(C.getUB().contains(element))) {
                B.remove(element, this);
            } else if (B.getLB().contains(element)) {
                C.force(element, this);
            } else if (C.getLB().contains(element)) {
                B.force(element,this);
            }
        };
        this.BForced = element -> {
            if (!(A.getUB().contains(element))) {
                C.force(element, this);
            } else if (!(C.getUB().contains(element))) {
                A.force(element, this);
            } else if (A.getLB().contains(element)) {
                C.remove(element, this);
            } else if (C.getLB().contains(element)) {
                A.remove(element,this);
            }
        };
        this.BRemoved = element -> {
            if (!(A.getUB().contains(element))) {
                C.remove(element, this);
            } else if (!(C.getUB().contains(element))) {
                A.remove(element, this);
            } else if (A.getLB().contains(element)) {
                C.force(element, this);
            } else if (C.getLB().contains(element)) {
                A.force(element,this);
            }
        };
        this.CForced = element -> {
            if (!(B.getUB().contains(element))) {
                A.force(element, this);
            } else if (!(A.getUB().contains(element))) {
                B.force(element, this);
            } else if (B.getLB().contains(element)) {
                A.remove(element, this);
            } else if (A.getLB().contains(element)) {
                B.remove(element,this);
            }
        };
        this.CRemoved = element -> {
            if (!(B.getUB().contains(element))) {
                A.remove(element, this);
            } else if (!(A.getUB().contains(element))) {
                B.remove(element, this);
            } else if (B.getLB().contains(element)) {
                A.force(element, this);
            } else if (A.getLB().contains(element)) {
                B.force(element,this);
            }
        };

    }

    //***********************************************************************************
    // METHODS
    //***********************************************************************************

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        if (PropagatorEventType.isFullPropagation(evtmask)) {
            ISetIterator iter = A.getUB().iterator();
            while (iter.hasNext()) {
                int e = iter.nextInt();
                if (B.getLB().contains(e) && C.getLB().contains(e)) {
                    A.remove(e, this);
                } else if (!(B.getUB().contains(e)) && !(C.getUB().contains(e))) {
                    A.remove(e, this);
                } else if (B.getLB().contains(e) && !(C.getUB().contains(e))) {
                    A.force(e, this);
                } else if (!(B.getUB().contains(e)) && C.getLB().contains(e)) {
                    A.force(e, this);
                }
            }

            iter = B.getUB().iterator();
            while (iter.hasNext()) {
                int e = iter.nextInt();
                if (A.getLB().contains(e) && C.getLB().contains(e)) {
                    B.remove(e, this);
                } else if (!(A.getUB().contains(e)) && !(C.getUB().contains(e))) {
                    B.remove(e, this);
                } else if (A.getLB().contains(e) && !(C.getUB().contains(e))) {
                    B.force(e, this);
                } else if (!(A.getUB().contains(e)) && C.getLB().contains(e)) {
                    B.force(e, this);
                }
            }

            iter = C.getUB().iterator();
            while (iter.hasNext()) {
                int e = iter.nextInt();
                if (A.getLB().contains(e) && B.getLB().contains(e)) {
                    C.remove(e, this);
                } else if (!(A.getUB().contains(e)) && !(B.getUB().contains(e))) {
                    C.remove(e, this);
                } else if (A.getLB().contains(e) && !(B.getUB().contains(e))) {
                    C.force(e, this);
                } else if (!(A.getUB().contains(e)) && B.getLB().contains(e)) {
                    C.force(e, this);
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
            sdm[idxVarInProp].forEach(AForced, SetEventType.ADD_TO_KER);
            sdm[idxVarInProp].forEach(ARemoved, SetEventType.REMOVE_FROM_ENVELOPE);
        } else if (idxVarInProp ==  1) {
            sdm[idxVarInProp].forEach(BForced, SetEventType.ADD_TO_KER);
            sdm[idxVarInProp].forEach(BRemoved, SetEventType.REMOVE_FROM_ENVELOPE);
        } else if (idxVarInProp ==  2) {
            sdm[idxVarInProp].forEach(CForced, SetEventType.ADD_TO_KER);
            sdm[idxVarInProp].forEach(CRemoved, SetEventType.REMOVE_FROM_ENVELOPE);
        }
    }

    @Override
    public ESat isEntailed() {

        if (A.getUB().size() == 0 && B.getUB().size() == 0 && C.getUB().size() == 0) {
            if (notEmpty) {
                return ESat.FALSE;
            } else {
                return ESat.TRUE;
            }
        }

        ISetIterator iter = A.getLB().iterator();
        while (iter.hasNext()) {
            int e = iter.nextInt();
            if (B.getLB().contains(e) && C.getLB().contains(e)) {
                return ESat.FALSE;
            } else if (!(B.getUB().contains(e)) && !(C.getUB().contains(e))) {
                return ESat.FALSE;
            }
        }

        iter = B.getLB().iterator();
        while (iter.hasNext()) {
            int e = iter.nextInt();
            if (A.getLB().contains(e) && C.getLB().contains(e)) {
                return ESat.FALSE;
            } else if (!(A.getUB().contains(e)) && !(C.getUB().contains(e))) {
                return ESat.FALSE;
            }
        }

        iter = C.getLB().iterator();
        while (iter.hasNext()) {
            int e = iter.nextInt();
            if (A.getLB().contains(e) && B.getLB().contains(e)) {
                return ESat.FALSE;
            } else if (!(A.getUB().contains(e)) && !(B.getUB().contains(e))) {
                return ESat.FALSE;
            }
        }

        if (isCompletelyInstantiated()) {
            return ESat.TRUE;
        }

        return ESat.UNDEFINED;
    }
}
