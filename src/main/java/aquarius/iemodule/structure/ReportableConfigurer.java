package aquarius.iemodule.structure;

import java.util.Map;

public interface ReportableConfigurer {
    Map<String, ReportableConfig> reportableConfig();

     class ReportableConfig{
        private final String altName;
        private final int order;

         private Class<? extends IEMapper<?,?>>  mapper;

         private Class<? extends HyperLinkHelper<?>> hyperLinkHelper;

        public ReportableConfig() {
            this("");
        }

        public ReportableConfig(String altName) {
            this(altName,Integer.MIN_VALUE);
        }

        public ReportableConfig(String altName, int order) {
            this.altName = altName;
            this.order = order;
        }

         public Class<? extends HyperLinkHelper<?>> getHyperLinkHelper() {
             return hyperLinkHelper;
         }

         public void setHyperLinkHelper(Class<? extends HyperLinkHelper<?>> hyperLinkHelper) {
             this.hyperLinkHelper = hyperLinkHelper;
         }

         public Class<? extends IEMapper<?, ?>> getMapper() {
             return mapper;
         }

         public void setMapper(Class<? extends IEMapper<?, ?>> mapper) {
             this.mapper = mapper;
         }

         public String getAltName() {
            return altName;
        }

        public int getOrder() {
            return order;
        }
    }
}
