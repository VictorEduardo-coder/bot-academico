package com.universidade.bot.model;

public class Disciplina {
    private Long id;
    private String nome;
    private int peso;
    private int dificuldade;
    private int horasSemanaisNecessarias;
    private String userId;

    public Disciplina() {}

    public Disciplina(Long id, String nome, int peso, int dificuldade, int horasSemanaisNecessarias, String userId) {
        this.id = id;
        this.nome = nome;
        this.peso = peso;
        this.dificuldade = dificuldade;
        this.horasSemanaisNecessarias = horasSemanaisNecessarias;
        this.userId = userId;
    }

    public double calcularFatorPrioridade() {
        return (this.peso * 0.6) + (this.dificuldade * 0.4);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public int getPeso() { return peso; }
    public void setPeso(int peso) { this.peso = peso; }

    public int getDificuldade() { return dificuldade; }
    public void setDificuldade(int dificuldade) { this.dificuldade = dificuldade; }

    public int getHorasSemanaisNecessarias() { return horasSemanaisNecessarias; }
    public void setHorasSemanaisNecessarias(int horas) { this.horasSemanaisNecessarias = horas; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    @Override
    public String toString() {
        return String.format("**%s** | Peso: %d | Dificuldade: %d | Horas/sem: %d | Prioridade: %.1f",
                nome, peso, dificuldade, horasSemanaisNecessarias, calcularFatorPrioridade());
    }
}
