package pt.ipt.dam.urbanaudit.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.io.Serializable;

/**
 * Entidade Room que modela os dados de uma Ocorrência/Auditoria Urbana na base de dados SQLite local.
 */
@Entity(tableName = "ocorrencias_locais")
public class Ocorrencia implements Serializable {

    @PrimaryKey(autoGenerate = true)
    private int idLocal;
    private Integer idServidor;
    private String titulo;
    private String descricao;
    private String categoria;
    private double latitude;
    private double longitude;
    private String endereco;
    private String caminhoFoto;
    private String fotoBase64;
    private String dataHora;
    private String autor;
    private String estado;
    private boolean sincronizado;

    public Ocorrencia() {
        this.categoria = "Geral";
        this.estado = "Pendente";
        this.sincronizado = false;
        this.endereco = "";
        this.caminhoFoto = "";
        this.fotoBase64 = "";
        this.dataHora = "";
        this.autor = "Utilizador";
    }

    @androidx.room.Ignore
    public Ocorrencia(String titulo, String descricao, String categoria, double latitude, double longitude,
                      String endereco, String caminhoFoto, String fotoBase64, String dataHora,
                      String autor, String estado, boolean sincronizado) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.categoria = (categoria == null || categoria.trim().isEmpty()) ? "Geral" : categoria;
        this.latitude = latitude;
        this.longitude = longitude;
        this.endereco = (endereco == null) ? "" : endereco;
        this.caminhoFoto = (caminhoFoto == null) ? "" : caminhoFoto;
        this.fotoBase64 = (fotoBase64 == null) ? "" : fotoBase64;
        this.dataHora = (dataHora == null) ? "" : dataHora;
        this.autor = (autor == null) ? "Utilizador" : autor;
        this.estado = (estado == null || estado.trim().isEmpty()) ? "Pendente" : estado;
        this.sincronizado = sincronizado;
    }

    public int getIdLocal() {
        return idLocal;
    }

    public void setIdLocal(int idLocal) {
        this.idLocal = idLocal;
    }

    public Integer getIdServidor() {
        return idServidor;
    }

    public void setIdServidor(Integer idServidor) {
        this.idServidor = idServidor;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public String getCaminhoFoto() {
        return caminhoFoto;
    }

    public void setCaminhoFoto(String caminhoFoto) {
        this.caminhoFoto = caminhoFoto;
    }

    public String getFotoBase64() {
        return fotoBase64;
    }

    public void setFotoBase64(String fotoBase64) {
        this.fotoBase64 = fotoBase64;
    }

    public String getDataHora() {
        return dataHora;
    }

    public void setDataHora(String dataHora) {
        this.dataHora = dataHora;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public boolean isSincronizado() {
        return sincronizado;
    }

    public void setSincronizado(boolean sincronizado) {
        this.sincronizado = sincronizado;
    }
}
