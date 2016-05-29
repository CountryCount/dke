package main.java;

public final class PopulationYear
{
    private final int year;
    private final int population;
    private final int wUnemployed;
    private final int mUnemployed;

    private final Commune commune;

    public PopulationYear(Commune commune, int year, int population, int wUnemployed, int mUnemployed)
    {
        if(commune == null)
            throw new IllegalArgumentException("commune");
        this.commune = commune;
        this.year = year;
        this.population = population;
        this.wUnemployed = wUnemployed;
        this.mUnemployed = mUnemployed;
    }

    public PopulationYear(Commune commune, int year, int population)
    {
        this(commune, year, population, 0, 0);
    }

    public Commune getCommune()
    {
        return commune;
    }

    public int getYear()
    {
        return year;
    }

    public int getPopulation()
    {
        return population;
    }

    public int getWUnemployed()
    {
        return wUnemployed;
    }

    public int getMUnemployed()
    {
        return mUnemployed;
    }

    public int getTotalUnemployed()
    {
        return getMUnemployed() + getWUnemployed();
    }
}
