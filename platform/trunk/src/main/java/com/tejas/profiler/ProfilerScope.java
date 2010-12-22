package com.tejas.profiler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.tejas.config.ApplicationConfig;
import com.tejas.profiler.ProfilerScope.ProfilerData.SingleScopeData;
import com.tejas.utils.misc.Assert;
import com.tejas.utils.misc.StringUtils;


public class ProfilerScope
{
    static class Builder
    {
        Long _count;
        String _name;
        boolean _nestedOnly;
        Long _timeSpent;

        public Builder(final String name)
        {
            Assert.notNull(name, "Profiler name can not be null");
            this._name = name.trim();
        }

        public ProfilerScope build()
        {
            return new ProfilerScope(this);
        }

        public Builder forCount(final long count)
        {
            this._count = count;
            return this;
        }

        public Builder forTimeSpent(final long timeSpent)
        {
            this._timeSpent = timeSpent;
            return this;
        }

        public Builder nestedOnly()
        {
            this._nestedOnly = true;
            return this;
        }
    }

    class ProfilerData
    {
        class SingleScopeData
        {
            Set<String> additionalData = new TreeSet<String>();
            long value;

            public SingleScopeData(final long t, final Set<String> additionalData)
            {
                this.value = t;
                this.additionalData = additionalData;
            }
        }

        List<SingleScopeData> siblings = new ArrayList<SingleScopeData>();
        public boolean isCounter;
        public String scopeName;

        @SuppressWarnings("synthetic-access")
        public ProfilerData(final ProfilerScope scope)
        {
            this.scopeName = scope.name;
            this.isCounter = scope.isCounter();
            merge(scope);
        }

        public void merge(final ProfilerScope scope)
        {
            @SuppressWarnings("synthetic-access")
            final SingleScopeData ssd =
                    new SingleScopeData(scope.isCounter() ? scope.totalCount : scope.totalTimeSpent, scope.additionalEntries);
            siblings.add(ssd);
        }

        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            sb.append(scopeName);
            sb.append(":");
            for (final SingleScopeData ssd : siblings)
            {
                sb.append(ssd.value);
                sb.append("$");
            }
            return sb.toString();
        }
    }

    private static final Logger logger = Logger.getLogger(ProfilerScope.class);
    private static ThreadLocal<ProfilerScope> rootScope = new ThreadLocal<ProfilerScope>();
    private final Set<String> additionalEntries = new TreeSet<String>();
    private final List<ProfilerScope> children = new Vector<ProfilerScope>();
    private final Long count;
    private final String name;
    private final boolean nestedOnly;
    private transient long numSiblings = 0;
    private final long startTime = System.currentTimeMillis();
    private transient long totalCount;
    private transient Long totalTimeSpent;

    ProfilerScope(final Builder builder)
    {
        this.name = builder._name;
        this.totalTimeSpent = builder._timeSpent;
        this.count = builder._count;
        this.nestedOnly = builder._nestedOnly;
        if (rootScope.get() == null)
        {
            rootScope.set(this);
        }
    }

    static void reset()
    {
        rootScope.set(null);
    }

    private void endScope()
    {
        final ProfilerScope root = rootScope.get();
        if (this != root)
        {
            root.children.add(this);
            return;
        }
        reset();
        if (nestedOnly)
        {
            /*
             * Don't output the data if this was supposed to be used as a nested-profiler.
             */
            return;
        }
        final Map<ProfilerScope, ProfilerData> family = new Hashtable<ProfilerScope, ProfilerData>();
        family.put(this, new ProfilerData(this));
        for (final ProfilerScope child : children)
        {
            final ProfilerData twin = family.get(child);
            if (twin == null)
            {
                family.put(child, new ProfilerData(child));
            }
            else
            {
                twin.merge(child);
            }
        }
        print(family.values());
    }

    private boolean isCounter()
    {
        return count != null;
    }

    private void print(final Collection<ProfilerData> values)
    {
        final List<ProfilerData> counters = new Vector<ProfilerData>();
        final List<ProfilerData> timers = new Vector<ProfilerData>();
        final List<String> additionalData = new ArrayList<String>();
        for (final ProfilerData data : values)
        {
            for (final SingleScopeData scope : data.siblings)
            {
                if (scope.additionalData.size() > 0)
                {
                    additionalData.addAll(scope.additionalData);
                }
            }
            if (data.isCounter)
            {
                counters.add(data);
            }
            else
            {
                timers.add(data);
            }
        }
        final StringBuilder inMemBuilder = new StringBuilder();
        final String appG = ApplicationConfig.getApplicationGroup();
        final String appN = ApplicationConfig.getApplicationName();
        inMemBuilder.append(appN).append("|");
        inMemBuilder.append(appG).append("|");
        inMemBuilder.append(this.name).append("|");
        inMemBuilder.append(this.startTime).append("|");
        inMemBuilder.append(this.totalTimeSpent).append("|");
        inMemBuilder.append(StringUtils.getCSVString(timers)).append("|");
        inMemBuilder.append(StringUtils.getCSVString(counters)).append("|");
        if (additionalData.size() > 0)
        {
            final String[] addData = new String[additionalData.size()];
            int i = 0;
            for (final String t : additionalData)
            {
                addData[i++] = t;
            }
            inMemBuilder.append(StringUtils.getCSVString(additionalData)).append("\n");
        }
        
        logger.info(inMemBuilder);
    }

    public void end()
    {
        /**
         * this.totalTimeSpent would be a non-null value when the ProfilerScope is initialized using Builder.forTimeSpent We should honor
         * that if that is the case.
         */
        if (this.totalTimeSpent == null)
        {
            this.totalTimeSpent = System.currentTimeMillis() - this.startTime;
        }
        this.totalCount = isCounter() ? this.count.longValue() : 0;
        endScope();
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (obj instanceof ProfilerScope)
        {
            final ProfilerScope that = (ProfilerScope) obj;
            return that.name.equals(this.name) && (that.isCounter() == this.isCounter());
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return name.hashCode();
    }

    public void insertAdditionalEntry(final String key, final String value)
    {
        this.additionalEntries.add(key + ":" + value);
    }

    @Override
    public String toString()
    {
        return this.name + ":" + (isCounter() ? "" + totalCount : "" + totalTimeSpent + "/" + (numSiblings + 1));
    }
}
