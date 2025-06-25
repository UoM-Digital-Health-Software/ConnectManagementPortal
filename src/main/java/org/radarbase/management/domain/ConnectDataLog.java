package org.radarbase.management.domain;


import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.envers.Audited;
import org.radarbase.management.domain.enumeration.DataGroupingType;
import org.radarbase.management.domain.support.AbstractEntityListener;

import javax.persistence.*;
import java.time.Instant;



@Entity
@Audited
@Table(name = "\"connect_data_log\"")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@EntityListeners({AbstractEntityListener.class})
public class ConnectDataLog {


    @Id
    @Column(name = "`userid`")
    private String userId;

    @Column(name = "datagroupingtype")
    private String dataGroupingType;

    @Column(name = "time")
    private Instant time;


    public Instant getTime() {
        return time;
    }

    public void setTime(Instant time) {
        this.time = time;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDataGroupingType() {
        return dataGroupingType;
    }

@Override
 public String toString() {
        return ("Group{"
                + "dataGroupingType=" + dataGroupingType.toString() + ", "
                + "userId=" + userId + ", "
                + "time=" + time + ", "
                + "}");
    }

    public void setDataGroupingType(DataGroupingType dataGroupingType) {
        this.dataGroupingType = dataGroupingType.toString();
    }
}

