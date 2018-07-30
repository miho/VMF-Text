
package eu.mihosoft.vmftext.java8.impl;

// vmf imports
import eu.mihosoft.vmf.runtime.core.*;
import eu.mihosoft.vmf.runtime.core.internal.*;
import eu.mihosoft.vcollections.*;
import eu.mihosoft.vmftext.java8.*;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;
import java.util.Objects;
import java.util.Arrays;

// property types imports

// implementation

/**
 * An implementation of the model object {@code eu.mihosoft.vmftext.java8.BreakStmnt}.
 */
@SuppressWarnings("deprecation")
class BreakStmntImpl implements BreakStmnt, VObjectInternalModifiable, VCloneableInternal {

    // --------------------------------------------------------------------
    // --- declaration of member variables
    // --------------------------------------------------------------------


    /*package private*/ CodeRange codeRange;
    /*package private*/ java.lang.String labelIdentifier;


// --------------------------------------------------------------------
// --- declaration of delegation variables
// --------------------------------------------------------------------


    private PropertyChangeSupport propertyChanges;

    // referenced by
    private final java.util.List<VObject> referencedBy = new java.util.ArrayList<>();
    // references
    private final java.util.List<VObject> references = new java.util.ArrayList<>();

    // --------------------------------------------------------------------
    // --- public constructors
    // --------------------------------------------------------------------

    public BreakStmntImpl() {

// --------------------------------------------------------------------
// --- declaration of delegation methods
// --------------------------------------------------------------------

    }

    // --------------------------------------------------------------------
    // --- public getter methods for accessing properties
    // --------------------------------------------------------------------


    @Override
    public CodeRange getCodeRange() {
        return this.codeRange;

    } // end of CodeRange getCodeRange()
    @Override
    public java.lang.String getLabelIdentifier() {
        return this.labelIdentifier;

    } // end of java.lang.String getLabelIdentifier()

    // --------------------------------------------------------------------
    // --- public setter methods for setting properties
    // --------------------------------------------------------------------


    @Override
    public void setCodeRange(CodeRange codeRange) {

        // return early if identical value has been set
        if (this.codeRange == codeRange) {
            return;
        }

        // set the new value
        CodeRange oldValue = this.codeRange;
        this.codeRange = codeRange;

        // fire property change event
        _vmf_firePropertyChangeIfListenersArePresent("codeRange", oldValue, this.codeRange);


        if(this.codeRange!=null && this!=null) {
            ((VObjectInternal)this)._vmf_references().add(this.codeRange);
            ((VObjectInternal)this.codeRange)._vmf_referencedBy().add(this);
        }

        if(oldValue!=null && this!=null) {
            ((VObjectInternal)this)._vmf_references().add(oldValue);
            ((VObjectInternal)oldValue)._vmf_referencedBy().add(this);
        }
    } // setterDeclaration (setter method)
    @Override
    public void setLabelIdentifier(java.lang.String labelIdentifier) {

        // return early if identical value has been set
        if (this.labelIdentifier == labelIdentifier) {
            return;
        }

        // set the new value
        java.lang.String oldValue = this.labelIdentifier;
        this.labelIdentifier = labelIdentifier;

        // fire property change event
        _vmf_firePropertyChangeIfListenersArePresent("labelIdentifier", oldValue, this.labelIdentifier);


    } // setterDeclaration (setter method)

    // --------------------------------------------------------------------
    // --- Object methods (equals(), toString() etc.)
    // --------------------------------------------------------------------


    // --------------------------- BEGIN TO_STRING -----------------------------
    @Override
    public String toString() {

        boolean entry = _vmf_getThreadLocalToString().get().isEmpty();
        try {
            // test if "this" has been seen before
            // implementation based on http://stackoverflow.com/a/11300376/1493549
            boolean isImmutable = (this instanceof eu.mihosoft.vmf.runtime.core.Immutable);
            if (!isImmutable && _vmf_getThreadLocalToString().get().containsKey(this)) {
                return "{skipping recursion}";
            } else {
                if(!isImmutable) {
                    _vmf_getThreadLocalToString().get().put(this, null);
                }
                entry = true;
            }
            return "{\"@type\":\"BreakStmnt\"" +
                    ", \"codeRange\": \"" + this.codeRange + "\"" +
                    ", \"labelIdentifier\": \"" + this.labelIdentifier + "\"" +
                    "}";
        } finally {
            if (entry) {
                _vmf_getThreadLocalToString().get().clear();
                _vmf_fToStringChecker = null;
            }
        }
    }

    private ThreadLocal<java.util.IdentityHashMap<BreakStmnt,?>> _vmf_getThreadLocalToString() {
        if (_vmf_fToStringChecker==null) {
            _vmf_fToStringChecker = ThreadLocal.withInitial(
                    () -> new java.util.IdentityHashMap<>());
        }

        return _vmf_fToStringChecker;
    }

    private ThreadLocal<java.util.IdentityHashMap<BreakStmnt, ?>> _vmf_fToStringChecker;

    // end toString()
    // ---------------------------- END TO_STRING ------------------------------


    // --------------------------- BEGIN EQUALITY -----------------------------
    @Override
    public boolean equals(Object o) {

        boolean entry = _vmf_getThreadLocalEquals().get().isEmpty();
        try {
            // test if the object pair (this,o) has been checked before
            boolean isImmutable = (o instanceof eu.mihosoft.vmf.runtime.core.Immutable);
            if (!isImmutable && _vmf_getThreadLocalEquals().get().containsKey(new EqualsPair(this, o))) {
                // This pair has been seen before. That's why we return true now.
                // If this pair wasn't equal, we wouldn't need to do a second
                // comparison. Returning 'true' is equivalent to ignoring this
                // particular test in the calling 'equals()' method.
                return true;
            } else {
                if(!isImmutable) {
                    _vmf_getThreadLocalEquals().get().put(new EqualsPair(this, o), null);
                }
                entry = true;
            }

            if (o==null) return false;
            else if (this==o) return true;

            // if object is read-only wrapper then unwrap the actual object
            if(o instanceof ReadOnlyBreakStmntImpl) {
                o = ((ReadOnlyBreakStmntImpl)o)._vmf_getMutableObject();
            }

            // -- try our interface/implementation --

            // perform the actual comparison for each property
            if (o instanceof BreakStmntImpl) {
                BreakStmntImpl other = (BreakStmntImpl) o;
                return           _vmf_equals(this.labelIdentifier, other.labelIdentifier);
            }

            // -- try every implementation that implements our interface --

            // no implementation matched. we end the comparison.
            return false;
        } finally {
            if (entry) {
                _vmf_getThreadLocalEquals().get().clear();
                _vmf_fEqualsChecker = null;
            }
        }
    } // end equals()

    private static boolean _vmf_equals(Object o1, Object o2) {
        boolean oneAndOnlyOneIsNull = (o1 == null) != (o2 == null);
        boolean collectionType = o1 instanceof VList || o2 instanceof VList;

        // since we support lazy initialization for collections,
        // uninitialized empty collection values are defined as equal to null
        // otherwise we would have to initialize these values, which would then
        // neutralize or even negate the positive effect of lazy initialization
        if(oneAndOnlyOneIsNull && collectionType) {
            if(o1==null) {
                return ((VList)o2).isEmpty();
            } else {
                return ((VList)o1).isEmpty();
            }
        } else {
            return Objects.equals(o1,o2);
        }
    }

    @Override
    public int hashCode() {
        boolean entry = _vmf_getThreadLocalHashCode().get().isEmpty();
        try {
            // test if "this class" has been seen before
            //
            // WARNING we use `System.identityHashCode(this)` to prevent recursive
            // hashCode() calls before we do the actual test. This would eliminate
            // the effect of the thread-local map
            if (_vmf_getThreadLocalHashCode().get().containsKey(System.identityHashCode(this))) {
                return 0; // already visited
            } else {
                _vmf_getThreadLocalHashCode().get().put(System.identityHashCode(this), null);
                int value = _vmf_deepHashCode(
                        this.labelIdentifier        );
                entry = true;
                return value;
            }

        } finally {
            if (entry) {
                _vmf_getThreadLocalHashCode().get().clear();
                _vmf_fHashCodeChecker = null;
            }
        }

    } // end hashCode()

    // fixes 'array discovery problems' with the 'Objects.hash(...)' method
    // see http://stackoverflow.com/questions/30385018/how-to-use-java-7-objects-hash-with-arrays
    private int _vmf_deepHashCode(Object... fields) {
        // WARNING we are not allowed to pass arrays that contain itself
        //         or are contained in nested arrays
        return Arrays.deepHashCode(fields);
    } // end _vmf_deepHashCode()

    /**
     * The purpose of this class is to store a pair of objects used for equals().
     * This class's equals() method checks equality by object identity. Same
     * for hashCode() which uses identity hashes of 'first' and 'second' to
     * compute the hash.
     *
     * This class can be used in conjunction with a regular HashMap to get
     * similar results to an IdentityHashMap, except that in this case identity
     * pairs can be used. And we don't have to use a map implementation that is
     * deliberately broken by design.
     */
    private static class EqualsPair {

        final Object first;
        final Object second;

        public EqualsPair(Object first, Object second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public int hashCode() {
            return Objects.hash(System.identityHashCode(first),
                    System.identityHashCode(second));
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final EqualsPair other = (EqualsPair) obj;
            if (this.first!=other.first) {
                return false;
            }
            if (this.second!=other.second) {
                return false;
            }
            return true;
        }
    }

    private ThreadLocal<java.util.Map<EqualsPair, ?>> _vmf_getThreadLocalEquals() {
        if (_vmf_fEqualsChecker==null) {
            _vmf_fEqualsChecker = ThreadLocal.withInitial(
                    () -> new java.util.HashMap<>());
        }

        return _vmf_fEqualsChecker;
    }

    private ThreadLocal<java.util.Map<Integer, ?>> _vmf_getThreadLocalHashCode() {
        if (_vmf_fHashCodeChecker==null) {
            _vmf_fHashCodeChecker = ThreadLocal.withInitial(
                    () -> new java.util.HashMap<>());
        }

        return _vmf_fHashCodeChecker;
    }

    private ThreadLocal<java.util.Map<EqualsPair, ?>> _vmf_fEqualsChecker;


    private ThreadLocal<java.util.Map<Integer, ?>> _vmf_fHashCodeChecker;
    // ---------------------------- END EQUALITY ------------------------------


    // --------------------------- BEGIN CLONING -----------------------------
    /**
     * Package private copy constructor.
     * It creates a deep or shallow copy of the specified other object.
     * @param other other object
     * @param deepCopy defines whether to perform a deep copy
     */
    BreakStmntImpl (
            BreakStmntImpl other,
            boolean deepCopy, java.util.IdentityHashMap<Object,Object> identityMap
    ) {
        identityMap.put(other,this);

        // property type is a model type
        if(deepCopy) {
            if(other.codeRange!=null) {
                this.setCodeRange((CodeRange)((VCloneableInternal)other.codeRange)._vmf_deepCopy(identityMap));
            }
        } else {
            this.codeRange = other.codeRange;
        }
        // property type is an external type (TODO implement cloning strategy)
        this.setLabelIdentifier(other.labelIdentifier);

    } // end copy constructor

    @Override
    public BreakStmntImpl _vmf_deepCopy(java.util.IdentityHashMap<Object,Object> identityMap) {
        if(identityMap.containsKey(this)) {
            return (BreakStmntImpl)identityMap.get(this);
        } else {
            BreakStmntImpl clonedVal = new BreakStmntImpl(this, true, identityMap);
            return clonedVal;
        }
    }

    @Override
    public BreakStmntImpl _vmf_shallowCopy(java.util.IdentityHashMap<Object,Object> identityMap) {
        if(identityMap.containsKey(this)) {
            return (BreakStmntImpl)identityMap.get(this);
        } else {
            BreakStmntImpl clonedVal = new BreakStmntImpl(this, false, identityMap);
            return clonedVal;
        }
    }
    @Override
    public BreakStmntImpl clone() /*throws CloneNotSupportedException*/ {
        // http://stackoverflow.com/questions/12886036/deep-copying-a-graph-structure
        // http://softwareengineering.stackexchange.com/questions/228848/how-does-java-handle-cyclic-data-references-when-serializing-an-object
        // https://gist.github.com/kanrourou/47223bdaf481505d4c7e
        // http://www.programcreek.com/2012/12/leetcode-clone-graph-java/
        java.util.IdentityHashMap<Object,Object> identityMap =
                new java.util.IdentityHashMap<>();
        return _vmf_deepCopy(identityMap);
    }
    // ---------------------------- END CLONING ------------------------------


// --------------------------------------------------------------------
// --- Builder methods
// --------------------------------------------------------------------

    public static class BuilderImpl implements BreakStmnt.Builder {

        private CodeRange codeRange;
        private java.lang.String labelIdentifier;

        private boolean appendCollections = true;

        public BuilderImpl() {}

        public BreakStmnt.Builder withCodeRange(CodeRange codeRange) {
            this.codeRange = codeRange;
            return this;
        }
        public BreakStmnt.Builder withLabelIdentifier(java.lang.String labelIdentifier) {
            this.labelIdentifier = labelIdentifier;
            return this;
        }

        public Builder appendCollections(boolean value) {
            this.appendCollections = value;
            return this;
        }

        public BreakStmnt build() {
            BreakStmntImpl result = new BreakStmntImpl();
            result.codeRange = this.codeRange;
            // PROP: codeRange

            if(result.codeRange!=null && result!=null) {
                ((VObjectInternal)result)._vmf_references().add(result.codeRange);
                ((VObjectInternal)result.codeRange)._vmf_referencedBy().add(result);
            }

            result.labelIdentifier = this.labelIdentifier;
            return result;
        }

        public Builder applyFrom(BreakStmnt o) {
            this.codeRange = o.getCodeRange();
            this.labelIdentifier = o.getLabelIdentifier();

            return this;
        }
        public Builder applyTo(BreakStmnt o) {

            o.setCodeRange(codeRange);
            o.setLabelIdentifier(labelIdentifier);

            return this;
        }
    } // end class BuilderImpl


// --------------------------------------------------------------------
// --- declaration of delegation methods
// --------------------------------------------------------------------


    // --------------------------------------------------------------------
    // --- Utility methods
    // --------------------------------------------------------------------

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l) {
        _vmf_getPropertyChanges().addPropertyChangeListener(l);
    }
    @Override
    public void removePropertyChangeListener(PropertyChangeListener l) {
        _vmf_getPropertyChanges().removePropertyChangeListener(l);

        if(_vmf_getPropertyChanges().getPropertyChangeListeners().length==0) {
            propertyChanges = null;
        }
    }

    private PropertyChangeSupport _vmf_getPropertyChanges() {

        if(propertyChanges==null) {
            propertyChanges = new PropertyChangeSupport(this);
        }

        return propertyChanges;
    }

    private boolean _vmf_hasListeners() {
        return propertyChanges!=null;
    }

    private void _vmf_firePropertyChangeIfListenersArePresent(
            String propertyName, Object oldValue, Object newValue) {
        if(_vmf_hasListeners()) {
            _vmf_getPropertyChanges().
                    firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    // --------------------------------------------------------------------
    // --- Public VMF API
    // --------------------------------------------------------------------

    private VMF vmf;

    @Override
    public VMF vmf() {
        if(vmf==null) {
            vmf = new VMF() {
                public Content content() {
                    return new Content() {
                        public java.util.Iterator<VObject> iterator() {
                            return VIterator.of(BreakStmntImpl.this);
                        }
                        public java.util.stream.Stream<VObject> stream() {
                            return VIterator.of(BreakStmntImpl.this).asStream();
                        }

                        public java.util.Iterator<VObject> iterator(VIterator.IterationStrategy strategy) {
                            return VIterator.of(BreakStmntImpl.this, strategy);
                        }
                        public java.util.stream.Stream<VObject> stream(VIterator.IterationStrategy strategy) {
                            return VIterator.of(BreakStmntImpl.this, strategy).asStream();
                        }

                        @Override
                        public BreakStmnt deepCopy() {
                            java.util.IdentityHashMap<Object,Object> identityMap =
                                    new java.util.IdentityHashMap<>();
                            return _vmf_deepCopy(identityMap);
                        }

                        @Override
                        public BreakStmnt shallowCopy() {
                            return BreakStmntImpl.
                                    this._vmf_shallowCopy(new java.util.IdentityHashMap<>());
                        }
                    };
                }

                private ChangesImpl changes;

                public Changes changes() {
                    if (changes==null) {
                        changes = new ChangesImpl();
                        changes.setModel(BreakStmntImpl.this);
                    }

                    return changes;
                }
            }; // end vmf
        } // end if null

        return vmf;
    }

    private ReadOnlyBreakStmnt readOnlyInstance;

    @Override
    public ReadOnlyBreakStmnt asReadOnly() {

        if(readOnlyInstance==null) {
            readOnlyInstance = new eu.mihosoft.vmftext.java8.impl.
                    ReadOnlyBreakStmntImpl(this);
        }

        return readOnlyInstance;
    }

    // --------------------------------------------------------------------
    // --- Reflection methods
    // --------------------------------------------------------------------

    // type id for improved reflection performance
    public static final int _VMF_TYPE_ID = 168;

    @Override
    public int _vmf_getTypeId() {
        return _VMF_TYPE_ID;
    }


    static final String[] _VMF_PROPERTY_NAMES = {
            "codeRange",
            "labelIdentifier"
    };

    static final int[] _VMF_PROPERTY_TYPES = {
            2, // type CodeRange
            -1  // type java.lang.String
    };

    /**
     * - indices of model objects as properties
     * - parents, i.e., containers are skipped
     * - only indices of reference properties and contained elements, i.e.,
     *   children are listed here
     */
    static final int[] _VMF_PROPERTIES_WITH_MODEL_TYPES_INDICES = {
            0, // type CodeRange
    };

    /**
     * - indices of lists that contain model objects as elements
     */
    static final int[] _VMF_PROPERTIES_WITH_MODEL_ELEMENT_TYPES_INDICES = {
    };

    /**
     * - indices of model objects as properties and model objects as
     *   elements of lists
     * - parents, i.e., containers are skipped
     * - only indices of reference properties and contained elements, i.e.,
     *   children are listed here
     */
    static final int[] _VMF_PROPERTIES_WITH_MODEL_TYPE_OR_ELEMENT_TYPES_INDICES = {
            0, // type CodeRange
    };

    /**
     * - indices of model children
     * - parents, i.e., containers and pure references are skipped
     * - only indices of contained elements, i.e.,
     *   children are listed here
     */
    static final int[] _VMF_CHILDREN_INDICES = {
    };

    @Override
    public String[] _vmf_getPropertyNames() {
        return _VMF_PROPERTY_NAMES;
    }

    @Override
    public int[] _vmf_getPropertyTypes() {
        return _VMF_PROPERTY_TYPES;
    }

    @Override
    public int[] _vmf_getIndicesOfPropertiesWithModelTypes() {
        return _VMF_PROPERTIES_WITH_MODEL_TYPES_INDICES;
    }

    @Override
    public int[] _vmf_getIndicesOfPropertiesWithModelElementTypes() {
        return _VMF_PROPERTIES_WITH_MODEL_ELEMENT_TYPES_INDICES;
    }

    @Override
    public int[] _vmf_getIndicesOfPropertiesWithModelTypeOrElementTypes() {
        return _VMF_PROPERTIES_WITH_MODEL_TYPE_OR_ELEMENT_TYPES_INDICES;
    }

    @Override
    public int[] _vmf_getChildrenIndices() {
        return _VMF_CHILDREN_INDICES;
    }

    @Override
    public Object _vmf_getPropertyValueById(int propertyId) {

        switch(propertyId) {
            case 0:
                // TODO check whether we can prevent lazy initialized properties from
                //      being initialized just for iterating the object graph
                return getCodeRange();
            case 1:
                // TODO check whether we can prevent lazy initialized properties from
                //      being initialized just for iterating the object graph
                return getLabelIdentifier();
        }

        return null;
    }

    @Override
    public int _vmf_getPropertyIdByName(String propertyName) {
        switch(propertyName) {
            case "codeRange":
                return 0;
            case "labelIdentifier":
                return 1;
            default:
                return -1;
        } // end switch
    }

    @Override
    public void _vmf_setPropertyValueById(int propertyId, Object value) {
        switch(propertyId) {
            case 0:
                setCodeRange((CodeRange)value);
                break;
            case 1:
                setLabelIdentifier((java.lang.String)value);
                break;
        } // end switch
    }

    // --------------------------------------------------------------------
    // --- Id related methods
    // --------------------------------------------------------------------

    // id management is currently not part of VMF (TODO how should we support this?)

    // --------------------------------------------------------------------
    // --- Reference methods
    // --------------------------------------------------------------------

    @Override
    public java.util.List<VObject> _vmf_referencedBy() { return this.referencedBy;}
    @Override
    public java.util.List<VObject> _vmf_references() { return this.references;}

}


class MyClass {
    public void testNumbers(){
        double v = 2.34 + 0x1.b7p-1;
    }
}