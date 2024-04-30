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
import org.chocosolver.util.objects.setDataStructures.ISetIterator;
import org.chocosolver.util.procedure.IntProcedure;

public class PropIndexUnion extends Propagator<SetVar> {
    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

    private final SetVar indices;
    private final SetVar elements;
    private final int offSet;
    private final int[][] relation;
    private final boolean notEmpty;
    private final ISetDeltaMonitor[] sdm;
    private final IntProcedure IForced;
    private final IntProcedure IRemoved;
    private final IntProcedure EForced;
    private final IntProcedure ERemoved;

    //***********************************************************************************
    // CONSTRUCTORS
    //***********************************************************************************

    /**
     * Does the union of the elements corresponding to the indices in relation
     *
     *
     * @param setInd set variable representing the indices
     * @param setEle set variable representing the elements to join
     * @param relation int[][]
     * @param notEmpty true : the set variable cannot be empty
     *                 false : the set may be empty
     */
    public PropIndexUnion(SetVar setInd, SetVar setEle, int[][] relation, boolean notEmpty) {
        this(setInd, setEle, 0, relation, notEmpty);
    }

    /**
     * Does the union of the sets of elements for which the indices are selected
     *
     *
     * @param setInd set variable representing the indices
     * @param setEle set variable representing the elements to join
     * @param offset int
     * @param relation int[][]
     * @param notEmpty true : the set variable cannot be empty
     *                 false : the set may be empty
     */
    public PropIndexUnion(SetVar setInd, SetVar setEle , int offset, int[][] relation, boolean notEmpty) {
        super(new SetVar[]{setInd, setEle}, PropagatorPriority.BINARY, true);
        this.indices = (SetVar) setInd;
        this.elements = (SetVar) setEle;
        this.offSet = offset;
        this.relation = relation;
        this.notEmpty = notEmpty;
        this.sdm = new ISetDeltaMonitor[]{this.indices.monitorDelta(this), this.elements.monitorDelta(this)};

        // PROCEDURES
        this.IForced = index -> {
            for (int e : get(index)) {
                elements.force(e, this);
            }
        };
        this.IRemoved = index -> {
            for (int e : get(index)) {
                boolean found = false;
                ISetIterator iterInd = indices.getUB().iterator();
                while (iterInd.hasNext()) {
                    int j = iterInd.nextInt();
                    if (contains(get(j), e)) {
                        found = true;
                        break;
                    }
                }
                if (!(found)) {
                    elements.remove(e, this);
                }
            }
        };
        this.EForced = element -> {
            ISetIterator iterInd = indices.getUB().iterator();
            boolean found = false;
            int index = 0;
            while (iterInd.hasNext()) {
                int j = iterInd.nextInt();
                if (contains(get(j), element)) {
                    if (found) {
                        found = false;
                        break;
                    } else {
                        found = true;
                        index = j;
                    }
                }
            }
            if (found) {
                indices.force(index, this);
            }
        };
        this.ERemoved = element -> {
            ISetIterator iterInd = indices.getUB().iterator();
            while (iterInd.hasNext()) {
                int j = iterInd.nextInt();
                if (contains(get(j), element)) {
                    indices.remove(j, this);
                }
            }
        };
    }

    //***********************************************************************************
    // METHODS
    //***********************************************************************************

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        if (PropagatorEventType.isFullPropagation(evtmask)) {
            ISetIterator iterEle = elements.getUB().iterator();
            while (iterEle.hasNext()) {
                boolean found = false;
                ISetIterator iterInd = indices.getUB().iterator();
                int k = iterEle.nextInt();
                while (iterInd.hasNext()) {
                    int j = iterInd.nextInt();
                    int[] element_list = get(j);
                    if (contains(element_list, k)) {
                        found = true;
                        break;
                    }
                }
                if (!(found)) {
                    elements.remove(k, this);
                }
            }

            ISetIterator iter = indices.getUB().iterator();
            while (iter.hasNext()) {
                int j = iter.nextInt();
                for (int e : get(j)) {
                    if (!(elements.getUB().contains(e))) {
                        indices.remove(j, this);
                        break;
                    }
                }
            }

            iter = indices.getLB().iterator();
            while (iter.hasNext()) {
                for (int e : get(iter.nextInt())) {
                    elements.force(e, this);
                }
            }

            iterEle = elements.getLB().iterator();
            while (iterEle.hasNext()) {
                ISetIterator iterInd = indices.getUB().iterator();
                int k = iterEle.nextInt();
                boolean found = false;
                int index = 0;
                while (iterInd.hasNext()) {
                    int j = iterInd.nextInt();
                    int[] element_list = get(j);
                    if (contains(element_list, k)) {
                        if (found) {
                            found = false;
                            break;
                        } else {
                            found = true;
                            index = j;
                        }
                    }
                }
                if (found) {
                    indices.force(index, this);
                }
            }
            sdm[0].startMonitoring();
            sdm[1].startMonitoring();
        }
    }

    @Override
    public void propagate(int idxVarInProp, int mask) throws ContradictionException {
        if (idxVarInProp ==  0) {
            sdm[idxVarInProp].forEach(IForced, SetEventType.ADD_TO_KER);
            sdm[idxVarInProp].forEach(IRemoved, SetEventType.REMOVE_FROM_ENVELOPE);
        } else if (idxVarInProp ==  1) {
            sdm[idxVarInProp].forEach(EForced, SetEventType.ADD_TO_KER);
            sdm[idxVarInProp].forEach(ERemoved, SetEventType.REMOVE_FROM_ENVELOPE);
        }
    }

    @Override
    public ESat isEntailed() {
        if (indices.getUB().size() == 0 && elements.getUB().size() == 0) {
            if (notEmpty) {
                return ESat.FALSE;
            } else {
                return ESat.TRUE;
            }
        }

        ISetIterator iter = indices.getLB().iterator();
        while (iter.hasNext()){
            for (int e : get(iter.nextInt())){
                if (!(elements.getUB().contains(e))) {
                    return ESat.FALSE;
                }
            }
        }

        ISetIterator iterEle = elements.getLB().iterator();
        while (iterEle.hasNext()){
            ISetIterator iterInd = indices.getUB().iterator();
            int k = iterEle.nextInt();
            boolean found = false;
            while (iterInd.hasNext()) {
                int j = iterInd.nextInt();
                int[] element_list = get(j);
                if (contains(element_list, k)) {
                    found = true;
                    break;
                }
            }
            if (!(found)) {
                return ESat.FALSE;
            }
        }

        if (isCompletelyInstantiated()) {
            return ESat.TRUE;
        }
        return ESat.UNDEFINED;
    }

    private int[] get(int j) {
        return relation[j - offSet];
    }

    private boolean contains(final int[] array, final int key) {
        for (final int i : array) {
            if (i == key) {
                return true;
            }
        }
        return false;
    }

}