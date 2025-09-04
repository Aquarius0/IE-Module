package aquarius.iemodule.structure;


import aquarius.iemodule.autoconfiguration.annotations.HyperLinkType;

public  interface HyperLinkHelper<T> {
    String returnHyperLink(T input);

    default HyperLinkType hyperLinkType(){return HyperLinkType.NONE;};
}
