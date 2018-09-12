/*************************************************************************************
 * Copyright (c) 2005, 2018 TragWerk Software Döking+Purtak GbR, Dresden, Germany
 * Author: Fuchs
 * All rights reserved.
 *************************************************************************************/
package org.jense.ktreemap;

/**
 * Provider of the KTreeMap
 *
 * @author dutheil_l
 */
public interface ITreeMapProvider {
    /**
     * get the label of the node
     *
     * @param node TreeMapNode
     * @return the label of the node
     */
    public String getLabel(TreeMapNode node);

    /**
     * Get the tooltip of the value.
     *
     * @param node the node
     * @return the tooltip of the node or <code>null</code>
     */
    public String getTooltip(TreeMapNode node);

    /**
     * Get the label of the value
     *
     * @param value value of the node (TreeMapNode.getValue())
     * @return the label of the value
     */
    public String getValueLabel(Object value);

    /**
     * Get the double value of the value
     *
     * @param value value of the node (TreeMapNode.getValue())
     * @return the double value of the value
     */
    public double getDoubleValue(Object value);

}
