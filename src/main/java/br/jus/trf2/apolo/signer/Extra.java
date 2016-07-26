package br.jus.trf2.apolo.signer;

import java.sql.Timestamp;

public class Extra {
	Timestamp dthrultatu;
	int pagecount;

	public Extra(String id) {
		String[] split = id.split("_");
		this.dthrultatu = new Timestamp(Long.valueOf(split[0]));
		if (this.dthrultatu.getTime() == 0L)
			this.dthrultatu = null;
		this.pagecount = Integer.valueOf(split[1]);
	}

	public Extra(Timestamp dthrultatu, int pagecount) {
		this.dthrultatu = dthrultatu;
		if (this.dthrultatu != null && this.dthrultatu.getTime() == 0L)
			this.dthrultatu = null;
		this.pagecount = pagecount;
	}

	public String toString() {
		return (dthrultatu == null ? "0" : dthrultatu.getTime()) + "_"
				+ pagecount;
	}
}
