/*
 * =============================================================================
 * 
 *   Copyright (c) 2011-2012, The THYMELEAF team (http://www.thymeleaf.org)
 * 
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 * =============================================================================
 */
package org.thymeleaf.dom;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.thymeleaf.Arguments;
import org.thymeleaf.Configuration;
import org.thymeleaf.processor.ProcessorAndContext;
import org.thymeleaf.processor.ProcessorResult;
import org.thymeleaf.util.IdentityCounter;
import org.thymeleaf.util.Validate;



/**
 * <p>
 *   Base abstract class for all nodes in a Thymeleaf DOM tree.
 * </p>
 * 
 * @author Daniel Fern&aacute;ndez
 * 
 * @since 2.0.0
 *
 */
public abstract class Node implements Serializable {

    private static final long serialVersionUID = 3082306990735650683L;
    
    private static final int DEFAULT_NODE_LOCAL_VARIABLES_MAP_SIZE = 3;

    /**
     * <p>
     *   Name of the property set by XML-based parsers into {@link Document} nodes
     *   for specifying the "XML Encoding" info from the XML Declaration.
     * </p>
     * <p>
     *   Value: "XML_ENCODING"
     * </p>
     */
    public static final String NODE_PROPERTY_XML_ENCODING = "XML_ENCODING";

    /**
     * <p>
     *   Name of the property set by XML-based parsers into {@link Document} nodes
     *   for specifying the "XML Version" info from the XML Declaration.
     * </p>
     * <p>
     *   Value: "XML_VERSION"
     * </p>
     */
    public static final String NODE_PROPERTY_XML_VERSION = "XML_VERSION";

    /**
     * <p>
     *   Name of the property set by XML-based parsers into {@link Document} nodes
     *   for specifying the "XML Standalone" flag from the XML Declaration.
     * </p>
     * <p>
     *   Value: "XML_STANDALONE"
     * </p>
     */
    public static final String NODE_PROPERTY_XML_STANDALONE = "XML_STANDALONE";
    
    private final String documentName;
    private final Integer lineNumber;
    
    private final boolean shouldConsiderAsElementForProcessing;
    
    NestableNode parent;
    private boolean skippable;
    private boolean precomputed;
    private boolean recomputeProcessorsAfterEachExecution;
    private boolean recomputeProcessorsImmediately;
    
    private HashMap<String,Object> nodeLocalVariables;

    private ArrayList<ProcessorAndContext> processors;

    private HashMap<String,Object> nodeProperties;
    

    /**
     * <p>
     *   Normalizes an element or attribute name by converting it
     *   to lower-case. Elements and attributes are processed as 
     *   case-insensitive, and this method allows normalizing their
     *   names before processing. 
     * </p>
     * 
     * @param name the name to be normalized.
     * @return the normalized name.
     */
    public static String normalizeName(final String name) {
        if (name == null) {
            return null;
        }
        return name.toLowerCase();
    }


    /**
     * <p>
     *   Applies a prefix (a dialect prefix) to the specified name.
     * </p>
     * <p>
     *   The result looks like: <tt>"${prefix}:${name}"</tt>.
     * </p>
     * 
     * @param name the name to be prefixed
     * @param dialectPrefix the prefix to be applied
     * @return the prefixed name
     */
    public static String applyDialectPrefix(final String name, final String dialectPrefix) {
        if (name == null) {
            return null;
        }
        if (dialectPrefix == null || dialectPrefix.trim().equals("")) {
            return name;
        }
        return dialectPrefix + ":" + name;
    }
    
    

    Node(final String documentName, final Integer lineNumber) {
        super();
        this.documentName = documentName;
        this.lineNumber = lineNumber;
        this.skippable = false;
        this.precomputed = false;
        this.recomputeProcessorsAfterEachExecution = false;
        this.recomputeProcessorsImmediately = false;
        this.nodeLocalVariables = null;
        this.processors = null;
        this.shouldConsiderAsElementForProcessing = (this instanceof Element || this instanceof Document);
        this.nodeProperties = null;
    }



    /**
     * <p>
     *   Returns the name of the document this node comes from. Can be null,
     *   if node does not come from parsing a document (for example, could come
     *   from parsing an externalized message).
     * </p>
     * 
     * @return the document name
     */
    public String getDocumentName() {
        return this.documentName;
    }
    

    /**
     * <p>
     *   Returns the number of the line where this node appears in the original template
     *   document. Might be null if the parser used to create the node is not able to
     *   obtain this information for nodes, or if this kind of information does not
     *   apply (for example, when parsing externalized messages).
     * </p>
     * 
     * @return the line number
     */
    public Integer getLineNumber() {
        return this.lineNumber;
    }

    

    /**
     * <p>
     *   Sets a value for a node property.
     * </p>
     * <p> 
     *   <i>Node properties</i> might contain arbitrary information, normally 
     *   set by parsers into node objects for use from processors. 
     * </p>
     * <p>
     *   Users might need to create parsers that add specific properties to some
     *   nodes in order to give processing hints to their processors or result
     *   writers. 
     * </p>
     * 
     * @param name the name of the property
     * @param value the value of the property
     */
    public final void setNodeProperty(final String name, final Object value) {
        Validate.notNull(name, "Property name cannot be null");
        if (this.nodeProperties == null) {
            this.nodeProperties = new HashMap<String,Object>();
        }
        this.nodeProperties.put(name, value);
    }

    
    /**
     * <p>
     *   Returns whether a node contains a specific <i>node property</i>
     *   or not.
     * </p>
     * <p> 
     *   <i>Node properties</i> might contain arbitrary information, normally 
     *   set by parsers into node objects for use from processors. 
     * </p>
     * <p>
     *   Users might need to create parsers that add specific properties to some
     *   nodes in order to give processing hints to their processors or result
     *   writers. 
     * </p>
     * 
     * @param name the name of the property to check
     * @return true if the node contains the property, false if not
     */
    public final boolean hasNodeProperty(final String name) {
        Validate.notNull(name, "Property name cannot be null");
        if (this.nodeProperties == null) {
            return false;
        }
        return this.nodeProperties.containsKey(name);
    }
    

    /**
     * <p>
     *   Returns the value of a specific <i>node property</i>, or <tt>null</tt>
     *   if the property has not been set.
     * </p>
     * <p> 
     *   <i>Node properties</i> might contain arbitrary information, normally 
     *   set by parsers into node objects for use from processors. 
     * </p>
     * <p>
     *   Users might need to create parsers that add specific properties to some
     *   nodes in order to give processing hints to their processors or result
     *   writers. 
     * </p>
     * 
     * @param name the name of the property to be retrieved
     * @return the value of the property
     */
    public final Object getNodeProperty(final String name) {
        Validate.notNull(name, "Property name cannot be null");
        if (this.nodeProperties == null) {
            return null;
        }
        return this.nodeProperties.get(name);
    }
    

    /**
     * <p>
     *   Returns the names of all the currently set <i>node properties</i>.
     * </p>
     * <p> 
     *   <i>Node properties</i> might contain arbitrary information, normally 
     *   set by parsers into node objects for use from processors. 
     * </p>
     * <p>
     *   Users might need to create parsers that add specific properties to some
     *   nodes in order to give processing hints to their processors or result
     *   writers. 
     * </p>
     * 
     * @return the set of property names.
     */
    public final Set<String> getNodePropertyNames() {
        if (this.nodeProperties == null) {
            return Collections.emptySet();
        }
        return Collections.synchronizedSet(this.nodeProperties.keySet());
    }
    

    /**
     * <p>
     *   Returns the real inner map object containing the node properties. This
     *   method <b>is only meant for internal use</b>. DO NOT call this
     *   method directly from your code.
     * </p>
     * 
     * @return the map of node properties.
     */
    public final Map<String,Object> unsafeGetNodeProperties() {
        return this.nodeProperties;
    }
    

    
    /**
     * <p>
     *   Returns whether this node has a parent node or not.
     * </p>
     * <p>
     *   A node does not have a parent if its <tt>parent</tt> property
     *   is set to null. {@link Document} nodes always have no parent, and
     *   other types of node might also have null parent if they represent
     *   the root of a DOM subtree.
     * </p>
     * 
     * @return true if the node has a parent, false if not.
     */
    public final boolean hasParent() {
        return this.parent != null;
    }

    
    /**
     * <p>
     *   Returns the parent of a node. Will return <tt>null</tt> if this
     *   node has no parent.
     * </p>
     * 
     * @return the parent of the node, or null if there is no parent.
     */
    public final NestableNode getParent() {
        return this.parent;
    }
    
    
    /**
     * <p>
     *   Sets a new parent for the node.
     * </p>
     * 
     * @param parent the new parent.
     */
    public final void setParent(final NestableNode parent) {
        this.parent = parent;
    }
    

    /**
     * <p>
     *   Returns the value of a flag indicating whether the list of processors to be applied
     *   to this node should be recomputed after the execution of each processor.
     * </p>
     * <p>
     *   This flag is usually set by processors with very high precedence in order to signal 
     *   the fact that they would like the engine to recompute the list of processors to be
     *   applied to the node after each of the subsequent processors finish their execution.
     * </p>
     * 
     * @return true if the list of applicable processors must be recomputed after each execution, 
     *              false if not. 
     */
    public final boolean getRecomputeProcessorsAfterEachExecution() {
        return this.recomputeProcessorsAfterEachExecution;
    }

    
    /**
     * <p>
     *   Sets the value of a flag indicating whether the processors to be applied
     *   to this node should be recomputed after the execution of each processor.
     * </p>
     * <p>
     *   This flag is usually set by processors with very high precedence in order to signal 
     *   the fact that they would like the engine to recompute the list of processors to be
     *   applied to the node after each of the subsequent processors finish their execution.
     * </p>
     * 
     * @param recomputeProcessorsAfterEachExecution the new value for the flag
     */
    public final void setRecomputeProcessorsAfterEachExecution(final boolean recomputeProcessorsAfterEachExecution) {
        this.recomputeProcessorsAfterEachExecution = recomputeProcessorsAfterEachExecution;
    }


    
    /**
     * <p>
     *   Returns the value of a flag indicating whether the processors to be applied
     *   to this node should be recomputed once after the execution of the current processor
     *   ends.
     * </p>
     * <p>
     *   This flag is usually set by processors in order to signal the fact that they have
     *   modified the node in a way that should modify the list of processors to be applied
     *   to it (for example, by adding an attribute which might have a processor associated).
     * </p>
     * <p>
     *   If this flag is set to true, the engine will automatically recompute the applicable
     *   processors before looking for the next processor to execute.
     * </p>
     * 
     * @return true if the list of applicable processors must be recomputed, false if not. 
     */
    public final boolean getRecomputeProcessorsImmediately() {
        return this.recomputeProcessorsImmediately;
    }


    /**
     * <p>
     *   Sets the value of a flag indicating whether the processors to be applied
     *   to this node should be recomputed once after the execution of the current processor
     *   ends.
     * </p>
     * <p>
     *   This flag is usually set by processors in order to signal the fact that they have
     *   modified the node in a way that should modify the list of processors to be applied
     *   to it (for example, by adding an attribute which might have a processor associated).
     * </p>
     * <p>
     *   If this flag is set to true, the engine will automatically recompute the applicable
     *   processors before looking for the next processor to execute.
     * </p>
     * 
     * @param recomputeProcessorsImmediately the new value for the flag
     */
    public final void setRecomputeProcessorsImmediately(final boolean recomputeProcessorsImmediately) {
        this.recomputeProcessorsImmediately = recomputeProcessorsImmediately;
    }

    
    
    /**
     * <p>
     *   Returns the value of a flag indicating whether there is any reason to process
     *   this node (and its children).
     * </p>
     * <p>
     *   This flag can be set by the engine -and also by processors- in order to avoid the
     *   execution of certain parts of the DOM tree, both because it is known that there is
     *   nothing to execute (and therefore it would be a waste of time trying to process it)
     *   and also because we want to signal that specific parts of the tree should not be 
     *   executed for security reasons (for example, to avoid code injection).
     * </p>
     * 
     * @return the value of the 'skippable' flag
     */
    public final boolean isSkippable() {
        return this.skippable;
    }
    

    /**
     * <p>
     *   Sets the value of a flag indicating whether there is any reason to process
     *   this node (and its children).
     * </p>
     * <p>
     *   This flag can be set by the engine -and also by processors- in order to avoid the
     *   execution of certain parts of the DOM tree, both because it is known that there is
     *   nothing to execute (and therefore it would be a waste of time trying to process it)
     *   and also because we want to signal that specific parts of the tree should not be 
     *   executed for security reasons (for example, to avoid code injection).
     * </p>
     * 
     * @param skippable the new value for the flag
     */
    public final void setSkippable(final boolean skippable) {
        this.skippable = skippable;
        if (!skippable && hasParent()) {
            // If this node is marked as non-skippable, set its parent as
            // non-skippable too.
            if (this.parent.isSkippable()) {
                this.parent.setSkippable(false);
            }
        }
        doAdditionalSkippableComputing(skippable);
    }
    
    abstract void doAdditionalSkippableComputing(final boolean isSkippable);


    final boolean isDetached() {
        if (this instanceof Document) {
            return false;
        }
        return !hasParent();
    }
    
    
    final boolean isPrecomputed() {
        return this.precomputed;
    }
    
    final void setPrecomputed(final boolean precomputed) {
        this.precomputed = precomputed;
    }

    
    
    
    /**
     * <p>
     *   Returns whether the node has any <i>node local variables</i>
     *   set or not.
     * </p>
     * <p>
     *   <i>Node local variables</i> are variables that are set locally to a specific
     *   node, and that will be added to the evaluation context of any
     *   expressions --for example: OGNL or Spring EL-- executed on that node or any
     *   of its children.
     * </p>
     * <p>
     *   For example, the <tt>thing</tt> variable in 
     *   <tt>&lt;div th:each="thing : ${things}"&gt;...&lt;div&gt;</tt>
     *   is a <i>node local variable</i> that will only exist inside the &lt;div&gt; element
     *   and that will be made available to any expressions executed within those limits.
     * </p>
     * <p>
     *   Note that there is an important difference between <i>node local variables</i>
     *   and <i>node properties</i>, as the latter are never added to the expression evaluation
     *   context and are only meant for internally carrying around metainformation about the
     *   DOM nodes.
     * </p>
     * 
     * @return true if the node has any node local variables set, false if not
     */
    public final boolean hasNodeLocalVariables() {
        return this.nodeLocalVariables != null && this.nodeLocalVariables.size() > 0;
    }

    
    /**
     * <p>
     *   Returns the set of <i>node local variable</i> names.
     * </p>
     * <p>
     *   <i>Node local variables</i> are variables that are set locally to a specific
     *   node, and that will be added to the evaluation context of any
     *   expressions --for example: OGNL or Spring EL-- executed on that node or any
     *   of its children.
     * </p>
     * <p>
     *   For example, the <tt>thing</tt> variable in 
     *   <tt>&lt;div th:each="thing : ${things}"&gt;...&lt;div&gt;</tt>
     *   is a <i>node local variable</i> that will only exist inside the &lt;div&gt; element
     *   and that will be made available to any expressions executed within those limits.
     * </p>
     * <p>
     *   Note that there is an important difference between <i>node local variables</i>
     *   and <i>node properties</i>, as the latter are never added to the expression evaluation
     *   context and are only meant for internally carrying around metainformation about the
     *   DOM nodes.
     * </p>
     * 
     * @return the set of local variable names
     */
    public final Set<String> getNodeLocalVariableNames() {
        if (this.nodeLocalVariables == null) {
            return Collections.emptySet();
        }
        return this.nodeLocalVariables.keySet();
    }

    
    /**
     * <p>
     *   Returns the real inner map object containing the node local variables. This
     *   method <b>is only meant for internal use</b>. DO NOT call this
     *   method directly from your code.
     * </p>
     * 
     * @return the map of node local variables
     */
    public final Map<String,Object> unsafeGetNodeLocalVariables() {
        return this.nodeLocalVariables;
    }


    /**
     * <p>
     *   Sets a new <i>node local variable</i>.
     * </p>
     * <p>
     *   <i>Node local variables</i> are variables that are set locally to a specific
     *   node, and that will be added to the evaluation context of any
     *   expressions --for example: OGNL or Spring EL-- executed on that node or any
     *   of its children.
     * </p>
     * <p>
     *   For example, the <tt>thing</tt> variable in 
     *   <tt>&lt;div th:each="thing : ${things}"&gt;...&lt;div&gt;</tt>
     *   is a <i>node local variable</i> that will only exist inside the &lt;div&gt; element
     *   and that will be made available to any expressions executed within those limits.
     * </p>
     * <p>
     *   Note that there is an important difference between <i>node local variables</i>
     *   and <i>node properties</i>, as the latter are never added to the expression evaluation
     *   context and are only meant for internally carrying around metainformation about the
     *   DOM nodes.
     * </p>
     * 
     * @param name the name of the local variable
     * @param value the new value for the local variable
     */
    public final void setNodeLocalVariable(final String name, final Object value) {
        if (this.nodeLocalVariables == null) {
            this.nodeLocalVariables = new HashMap<String, Object>(DEFAULT_NODE_LOCAL_VARIABLES_MAP_SIZE);
        }
        this.nodeLocalVariables.put(name,  value);
    }

    
    /**
     * <p>
     *   Sets several <i>node local variables</i> at once.
     * </p>
     * <p>
     *   <i>Node local variables</i> are variables that are set locally to a specific
     *   node, and that will be added to the evaluation context of any
     *   expressions --for example: OGNL or Spring EL-- executed on that node or any
     *   of its children.
     * </p>
     * <p>
     *   For example, the <tt>thing</tt> variable in 
     *   <tt>&lt;div th:each="thing : ${things}"&gt;...&lt;div&gt;</tt>
     *   is a <i>node local variable</i> that will only exist inside the &lt;div&gt; element
     *   and that will be made available to any expressions executed within those limits.
     * </p>
     * <p>
     *   Note that there is an important difference between <i>node local variables</i>
     *   and <i>node properties</i>, as the latter are never added to the expression evaluation
     *   context and are only meant for internally carrying around metainformation about the
     *   DOM nodes.
     * </p>
     * 
     * @param variables the variables to be set
     */
    public final void setAllNodeLocalVariables(final Map<String,Object> variables) {
        if (variables != null) {
            if (this.nodeLocalVariables == null) {
                this.nodeLocalVariables = new HashMap<String, Object>(variables);
            } else {
                this.nodeLocalVariables.putAll(variables);
            }
        }
    }

    
    final void unsafeSetNodeLocalVariables(final HashMap<String,Object> variables) {
        if (variables != null) {
            this.nodeLocalVariables = variables;
        } else { 
            this.nodeLocalVariables = null;
        }
    }



    
    final void precomputeNode(final Configuration configuration) {

        if (!isPrecomputed()) {

            /*
             * Compute the processors that are applicable to this node
             */
            this.processors = configuration.computeProcessorsForNode(this);

            
            /*
             * Set skippability
             */
            if (this.processors == null || this.processors.size() == 0) {
                // We only set this specific node as skippable. If we executed
                // "setSkippable", the whole tree would be set as skippable, which
                // is unnecessary due to the fact that we are going to precompute
                // all of this node's children in a moment.
                // Also, note that if any of this node's children has processors
                // (and therefore sets itself as "non-skippable"), it will also
                // set its parent as non-skippable, overriding this action.
                this.skippable = true;
            } else {
                // This time we execute "setSkippable" so that all parents at all
                // levels are also set to "false"
                setSkippable(false);
            }

            
            /*
             * Set the "precomputed" flag to true 
             */
            setPrecomputed(true);

        }
        
        
        /*
         * Let subclasses add their own preprocessing
         */
        doAdditionalPrecomputeNode(configuration);
     
    }
    
    
    abstract void doAdditionalPrecomputeNode(final Configuration configuration);

    
    
    
    
    void processNode(final Arguments arguments, final boolean processOnlyElementNodes) {

        if (!this.shouldConsiderAsElementForProcessing && processOnlyElementNodes) {
            return;
        }
        
        if (this.recomputeProcessorsImmediately || this.recomputeProcessorsAfterEachExecution) {
            precomputeNode(arguments.getConfiguration());
            this.recomputeProcessorsImmediately = false;
        }
        
        if (!isPrecomputed()) {
            precomputeNode(arguments.getConfiguration());
        }

        if (!isSkippable()) {
                        
            /*
             *  If there are local variables at the node, add them to the ones at the
             *  Arguments object.
             */
            Arguments executionArguments =
                    (this.nodeLocalVariables != null && this.nodeLocalVariables.size() > 0?
                            arguments.addLocalVariables(this.nodeLocalVariables) : arguments);
            
            /* 
             * If the Arguments object has local variables, synchronize the node-local
             * variables map.
             */
            if (executionArguments.getExpressionEvaluationContext().hasLocalVariables()) {
                unsafeSetNodeLocalVariables(executionArguments.getExpressionEvaluationContext().unsafeGetLocalVariables());
            }
            
            /*
             * Perform the actual processing
             */
            if (!isDetached() && this.processors != null && this.processors.size() > 0) {
                
                final IdentityCounter<ProcessorAndContext> alreadyExecuted = 
                        new IdentityCounter<ProcessorAndContext>(this.processors.size());
                Arguments processingArguments = executionArguments;

                while (!isDetached() && processingArguments != null) {
                    
                    // This way of executing processors allows processors to perform updates
                    // that might change which processors should be applied (for example, by
                    // adding or removing attributes)
                    processingArguments = 
                            applyNextProcessor(processingArguments, this, alreadyExecuted);
                    
                    if (processingArguments != null) {
                        // if we didn't reach the end of processor executions, update
                        // the Arguments object being used for processing
                        executionArguments = processingArguments;
                    }
                    
                    if (this.recomputeProcessorsImmediately || this.recomputeProcessorsAfterEachExecution) {
                        precomputeNode(arguments.getConfiguration());
                        this.recomputeProcessorsImmediately = false;
                    }
                    
                }
                
            }
            
            doAdditionalProcess(executionArguments, executionArguments.getProcessOnlyElementNodes());
            
        }
    
    }
    
    
    
    
    
    private static final Arguments applyNextProcessor(final Arguments arguments, final Node node, final IdentityCounter<ProcessorAndContext> alreadyExecuted) {

        if (!node.isDetached() && node.processors != null && node.processors.size() > 0) {

            for (final ProcessorAndContext processor : node.processors) {
                
                if (!alreadyExecuted.isAlreadyCounted(processor)) {
                    
                    Arguments executionArguments = arguments;

                    final ProcessorResult processorResult = 
                            processor.getProcessor().process(executionArguments, processor.getContext(), node);
                    
                    // The execution arguments need to be updated as instructed by the processor
                    // (for example, for adding local variables)
                    executionArguments = processorResult.computeNewArguments(executionArguments);
                    
                    // If we have added local variables, we should update the node's map for these variables in
                    // order to keep them synchronized
                    if (processorResult.hasLocalVariables() && executionArguments.getExpressionEvaluationContext().hasLocalVariables()) {
                        node.unsafeSetNodeLocalVariables(executionArguments.getExpressionEvaluationContext().unsafeGetLocalVariables());
                    }
                    
                    // Make sure this specific processor instance is not executed again
                    alreadyExecuted.count(processor);
                    
                    return executionArguments;
                    
                }
                
            }
            
        }

        // Either there are no processors, or all of them have already been processed
        return null;
        
    }
    
    
    
    abstract void doAdditionalProcess(final Arguments arguments, final boolean processOnlyElementNodes);
    
    

    /**
     * <p>
     *   Creates a clone of this node.
     * </p>
     * <p>
     *   When cloning, it can be specified whether we want to clone also the available
     *   <i>preprocessing</i> information (the lists of processors that should be applicable
     *   to each node), and also the new parent to be assigned to the DOM tree resulting from
     *   cloning (parent can be null).
     * </p>
     * <p>
     *   <b>Node cloning is always performed in depth</b>. 
     * </p>
     * 
     * @param newParent the new parent node of the resulting cloned DOM tree, if any (can be specified
     *                  as null).
     * @param cloneProcessors whether the lists of applicable processors for each node should
     *                        also be cloned or not.
     * @return the cloned node.
     */
    public final Node cloneNode(final NestableNode newParent, final boolean cloneProcessors) {
        final Node clone = createClonedInstance(newParent, cloneProcessors);
        cloneNodeInternals(clone, newParent, cloneProcessors);
        return clone;
    }
    
    

    abstract Node createClonedInstance(final NestableNode newParent, final boolean cloneProcessors);
    
    
    final void cloneNodeInternals(final Node node, final NestableNode newParent, final boolean cloneProcessors) {
        doCloneNodeInternals(node, newParent, cloneProcessors);
        if (cloneProcessors) {
            node.processors = this.processors;
            node.skippable = this.skippable;
            node.precomputed = this.precomputed;
        } else {
            node.processors = null;
            node.skippable = false;
            node.precomputed = false;
        }
        node.parent = newParent;
        if (this.nodeLocalVariables != null) {
            node.nodeLocalVariables = new HashMap<String,Object>(this.nodeLocalVariables);
        }
        if (this.nodeProperties != null) {
            node.nodeProperties = new HashMap<String,Object>(this.nodeProperties);
        }
    }

    
    abstract void doCloneNodeInternals(final Node node, final NestableNode newParent, final boolean cloneProcessors);
    
    

    /**
     * <p>
     *   Apply a DOM visitor.
     * </p>
     * 
     * @param visitor the visitor to be executed for this node.
     */
    public abstract void visit(final DOMVisitor visitor);

    
    

}
