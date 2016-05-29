package main.java;

import java.util.LinkedList;
import java.util.List;

public final class Commune
{
    private final String nuts2;
    private final String lau2_Code;
    private final String lau2_Name;

    private final List<PopulationYear> years;

    public Commune(String lau2_Name, String lau2_Code, String nuts2)
    {
        this.lau2_Name = lau2_Name;
        this.lau2_Code = lau2_Code;
        this.nuts2 = nuts2;
        years = new LinkedList<>();
    }

    public String getNuts2()
    {
        return nuts2;
    }

    public String getLau2_Code()
    {
        return lau2_Code;
    }

    public String getLau2_Name()
    {
        return lau2_Name;
    }

    public List<PopulationYear> getYears()
    {
        return years;
    }
}
