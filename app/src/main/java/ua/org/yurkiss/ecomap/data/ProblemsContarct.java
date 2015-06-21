package ua.org.yurkiss.ecomap.data;

import android.provider.BaseColumns;

/**
 * Created by yridk_000 on 21.06.2015.
 */
public class ProblemsContarct{

    public static class ProblemEntry implements BaseColumns {

        public static final String TABLE_NAME = "problems";

        public static final String COLUMN_ECOMAP_ID = "ecomap_id";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_LATITUDE = "latitude";
        public static final String COLUMN_LONGTITUDE = "longtitude";
        public static final String COLUMN_PROBLEMTYPEID = "problem_types_id";
        public static final String COLUMN_STATUS = "status";
        public static final String COLUMN_DATE = "date";

    }
}
