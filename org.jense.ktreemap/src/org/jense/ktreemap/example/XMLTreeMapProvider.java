package org.jense.ktreemap.example;

import org.jense.ktreemap.ITreeMapProvider;
import org.jense.ktreemap.TreeMapNode;

/**
 * TreeMapProvider for a XML file
 */
public class XMLTreeMapProvider implements ITreeMapProvider {

    @Override
    public double getDoubleValue(Object value) {
        if (value instanceof XMLBean) {
            XMLBean bean = (XMLBean) value;
            return bean.getValue();
        }
        return 0;
    }

    @Override
    public String getLabel(TreeMapNode node) {
        Object value = node.getValue();
        if (value instanceof XMLBean) {
            XMLBean bean = (XMLBean) value;
            return bean.getLabel();
        }
        return null;
    }

    @Override
    public String getValueLabel(Object value) {
        return "" + getDoubleValue(value);
    }

    @Override
    public String getTooltip(TreeMapNode node) {
        return getLabel(node) + "\n" + getValueLabel(node.getValue());
    }

}
