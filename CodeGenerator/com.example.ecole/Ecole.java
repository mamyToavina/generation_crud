package com.example.ecole;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import model.position;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;


@Entity
public class ecole{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idecole;
    private String nom;
    @ManyToOne;
    @JoinColumn(name = id)
    private position idposition;


    public ecole(){
    }

    public ecole(Long idecole, String nom, position idposition){
       this.idecole = idecole;
       this.nom = nom;
       this.idposition = idposition;
       
    }
    
    public Long getIdecole() {
       return idecole;
    }

    public String getNom() {
       return nom;
    }

    public position getIdposition() {
       return idposition;
    }


    public void setIdecole(Long idecole) {
        this.idecole = idecole;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setIdposition(position idposition) {
        this.idposition = idposition;
    }

    

}
