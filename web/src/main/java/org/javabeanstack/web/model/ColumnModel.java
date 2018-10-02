package org.javabeanstack.web.model;

import java.io.Serializable;

/**
 * Modelo de columnas utilizado para manejar dataTables con columnas dinamicas
 * @author mtrinidad
 */ 
public class ColumnModel implements Serializable {

    private String header;
    private Integer property;
    private String column;
    private Boolean visible;
    private String link;
    private String id;
    private String style;
    private String filterFunction;
    private int width;
    private String mask;

    public ColumnModel() {
    }

    public ColumnModel(String header, Integer property, String column, String style, int width,String link, String filterFunction, String mask) {
        this.header = header;        
        this.property = property;        
        this.column = column;
        this.style= style;
        this.width = width;
        this.link=link;
        this.filterFunction=filterFunction;
        this.mask=mask;
    }

    public ColumnModel(String header, Integer property, String column, Boolean visible,String link, String id) {
        this.header = header;
        this.property = property;
        this.column = column;
        this.visible= visible;
        this.link  = link;
        this.id=id;
    }    
    
    public ColumnModel(String header, Integer property, String column, Boolean visible,String link, String id,String mask) {
        this.header = header;
        this.property = property;
        this.column = column;
        this.visible= visible;
        this.link  = link;
        this.id=id;
        this.mask=mask;
    }

    /**
     * Devuelve encabezado de la columna
     * @return 
     */
    public String getHeader() {
        return header;
    }

    /**
     * Devuelve el nro de elemento al cual corresponde una columna
     * @return 
     */
    public Integer getProperty() {
        return property;
    }

    /**
     * Establece la cabecera para una columna
     * @param header 
     */
    public void setHeader(String header) {
        this.header = header;
    }

    /**
     * Establece el nro de elemento al cual corresponde una columna
     * @param property 
     */
    public void setProperty(Integer property) {
        this.property = property;
    }

    /**
     * Devuelve nombre de la columna, esto cuando se tienen valores fijos y no dinamicos
     * @return 
     */
    public String getColumn() {
        return column;
    }

    /**
     * Establece valores de la columna, cuando se tienen valores fijos y no dinamicos
     * @param column 
     */
    public void setColumn(String column) {
        this.column = column;
    }    

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }    

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }    

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public String getFilterFunction() {
        return filterFunction;
    }

    public void setFilterFunction(String filterFunction) {
        this.filterFunction = filterFunction;
    }

    public String getMask() {
        return mask;
    }

    public void setMask(String mask) {
        this.mask = mask;
    }


    @Override
    public String toString() {
        return this.getHeader();
    }
}
