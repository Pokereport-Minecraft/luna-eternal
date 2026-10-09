package net.pokereport.luna.crianza;

/** Paid expansions and rank benefits are separate permissions. Indices are zero based. */
public final class CrianzaRanuras {
    private CrianzaRanuras() {}

    public static boolean disponible(int indice, int escalon, boolean comprada, int rangoRequerido) {
        if (indice < 0 || indice > 6) return false;
        if (indice < 2) return true;
        if (indice < 5) return comprada;
        return rangoRequerido >= 0 && escalon >= rangoRequerido;
    }
}
