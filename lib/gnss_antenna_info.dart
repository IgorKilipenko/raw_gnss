class GnssAntennaInfo {
  final String? id;
  final int contents;
  final double carrierFrequencyMHz;
  final List<PhaseCenterOffset> phaseCenterOffset;
  final Map<String, dynamic> phaseCenterVariationCorrections;

  // factory GnssAntennaInfo.fromJson(Map<String, dynamic> json) {
  // return GnssAntennaInfo();
  // }

  GnssAntennaInfo({
    this.id,
    required this.contents,
    required this.carrierFrequencyMHz,
    required this.phaseCenterOffset,
    required this.phaseCenterVariationCorrections
  });
}

class PhaseCenterOffset {
  final double xOffsetMm;
  final double xOffsetUncertaintyMm;
  final double yOffsetMm;
  final double yOffsetUncertaintyMm;
  final double zOffsetMm;
  final double zOffsetUncertaintyMm;

  PhaseCenterOffset(
      {required this.xOffsetMm,
      required this.xOffsetUncertaintyMm,
      required this.yOffsetMm,
      required this.yOffsetUncertaintyMm,
      required this.zOffsetMm,
      required this.zOffsetUncertaintyMm});

  factory PhaseCenterOffset.fromJson(Map<String, double> json) {
    PhaseCenterOffset? instance;
    Object? error;
    try {
      instance = PhaseCenterOffset(
        xOffsetMm: json['xOffsetMm']!,
        xOffsetUncertaintyMm: json['xOffsetUncertaintyMm']!,
        yOffsetMm: json['yOffsetMm']!,
        yOffsetUncertaintyMm: json['yOffsetUncertaintyMm']!,
        zOffsetMm: json['zOffsetMm']!,
        zOffsetUncertaintyMm: json['zOffsetUncertaintyMm']!,
      );
    } catch (e, s) {
      error = e;
      print('Exception parsing PhaseCenterOffset, error: $e\nstack: $s');
    }

    if (error != null) {
      throw new Exception(error);
    }

    return instance!;
  }

  @override
  String toString() {
    return 'PhaseCenterOffset{OffsetXMm=$xOffsetMm +/-$xOffsetUncertaintyMm, OffsetYMm=$yOffsetMm +/-$yOffsetUncertaintyMm, OffsetZMm=$zOffsetMm +/-$zOffsetUncertaintyMm}';
  }
}
