package org.javabeanstack.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import org.javabeanstack.data.IDataRow;


/**
 *
 * @author Jorge Enciso
 */
public interface IAppUser extends IDataRow, Serializable {
    public static final String ANALISTA = "00";
    public static final String SUPERUSER = "01";
    public static final String ADMINISTRADOR = "20";
    public static final String ADMINISTRADORSYSTEM = "20";
    public static final String ADMINCOMPANY = "21";
    public static final String TOKEN = "25";    
    public static final String USUARIO = "30";    
    
    public static final Short ISUSER = 1;
    public static final Short ISUSERGROUP = 2;
    
    Long getIduser();    
    String getLogin();
    String getCode();
    String getFullName();
    String getDescription();
    
    String getPass();
    String getPassBackup();
    
    String getPassConfirm();
    String getPassConfirm2();
    
    
    Boolean getDisabled();
    LocalDateTime getExpiredDate();
    
    Long getIdcompany();        
    List<IAppCompanyAllowed> getAppCompanyAllowedList();
    List<IAppUserMember> getUserMemberList();
    
    String getRol();
    String getHighRol();
    String getAllRoles();    
    String getAppRol();    
    Short getType();
    byte[] getAvatar();
    
    void setIduser(Long iduser);    
    void setLogin(String loginName);
    void setCode(String code);
    void setFullName(String name);    
    void setDescription(String description);    
    
    void setPass(String password);
    void setPassConfirm(String passwordConfirm);
    void setPassConfirm2(String passwordConfirm2);    

    void setIdcompany(Long idcompany);    
    void setAppCompanyAllowedList(List<IAppCompanyAllowed> appCompanyAllowedList);
    void setUserMemberList(List<IAppUserMember> userMemberList);
    
    void setDisabled(Boolean disable);
    void setExpiredDate(LocalDateTime expira);

    void setRol(String rol);
    void setAppRol(String appRol);    
    void setType(Short tipo);
    void setAvatar(byte[] avatar);
    boolean isSysAdmin();
    boolean isCompanyAdmin();    
    boolean isSuperUser();

    /**
     * Devuelve las direcciones IP desde las cuales este usuario tiene
     * permitido ingresar.
     *
     * <p>Es una lista separada por comas, con la misma sintaxis de los
     * parámetros {@code IP_REQUEST_ALLOWED} e {@code IP_REQUEST_NOT_ALLOWED}
     * del sistema: direcciones exactas y comodines por octeto, de modo que
     * {@code 192.168.*} habilita toda esa red. La evalúa
     * {@code org.javabeanstack.util.Fn.ipMatch}, que vive en otro módulo del
     * framework: por eso va como {@code code} y no como enlace.</p>
     *
     * <p><b>En blanco o nulo significa desde cualquier dirección</b>, que es
     * el equivalente de {@code 0.0.0.0} y el comportamiento histórico. La
     * restricción es por usuario y se evalúa <b>además</b> de las dos listas
     * del sistema, que siguen aplicándose a toda la instalación.</p>
     *
     * @return lista de direcciones permitidas, o nulo si no hay restricción.
     */
    default String getIpLoginAllowed() {
        return null;
    }

    /**
     * Asigna las direcciones IP desde las cuales el usuario puede ingresar.
     *
     * <p>Tiene cuerpo vacío por omisión para que las proyecciones de usuario
     * que no mapean la columna no queden obligadas a implementarlo.</p>
     *
     * @param ipLoginAllowed lista de direcciones o patrones separados por coma.
     */
    default void setIpLoginAllowed(String ipLoginAllowed) {
    }
}
