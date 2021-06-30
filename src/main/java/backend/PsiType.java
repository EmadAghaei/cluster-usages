package backend;

public enum PsiType {

    CLAZZ("PSIClass"),
    FIELD("PSIField"),
    STATEMENT("Statement");

    private final String psi;


    PsiType(String psiElement) {
        this.psi=psiElement;
    }
    public String toString(){
        return this.psi;
    }
}

