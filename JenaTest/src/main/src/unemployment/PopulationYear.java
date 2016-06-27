package unemployment;

public final class PopulationYear
{
    private final int year;
    private final int population;
    private int wUnemployed;
    private int mUnemployed;

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
        this(commune, year, population, -1, -1);
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

    public void setWUnemployed(int wUnemployed)
    {
        this.wUnemployed = wUnemployed;
    }

    public int getMUnemployed()
    {
        return mUnemployed;
    }

    public void setMUnemployed(int mUnemployed)
    {
        this.mUnemployed = mUnemployed;
    }

    public int getTotalUnemployed()
    {
        return getMUnemployed() + getWUnemployed();
    }
}
