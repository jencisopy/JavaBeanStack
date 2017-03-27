package org.javabeanstack.model;

import org.javabeanstack.data.IDataRow;

/**
 *
 * @author Jorge Enciso
 */
public interface IAppMessages extends IDataRow {
    Long getIdmessage();
    Long getNumber();
    String getText();
    String getExplanation();
}
