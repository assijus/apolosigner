package br.jus.trf2.apolo.signer;

import java.sql.Timestamp;

public class Id {
	String cpf;
	int codsecao;
	long coddoc;
	Timestamp dthrmov;
	Timestamp dthrultatu;
	int pagecount;

	public Id(String id) {
		String[] split = id.split("_");
		this.cpf = split[0];
		this.codsecao = Integer.valueOf(split[1]);
		this.coddoc = Long.valueOf(split[2]);
		this.dthrmov = new Timestamp(Long.valueOf(split[3]));
		this.dthrultatu = new Timestamp(Long.valueOf(split[4]));
		if (this.dthrultatu.getTime() == 0L)
			this.dthrultatu = null;
		this.pagecount = Integer.valueOf(split[5]);
	}

	public Id(String cpf, int codsecao, long coddoc, Timestamp dthrmov,
			Timestamp dthrultatu, int pagecount) {
		this.cpf = cpf;
		this.codsecao = codsecao;
		this.coddoc = coddoc;
		this.dthrmov = dthrmov;
		this.dthrultatu = dthrultatu;
		if (this.dthrultatu != null && this.dthrultatu.getTime() == 0L)
			this.dthrultatu = null;
		this.pagecount = pagecount;
	}

	public String toString() {
		return cpf + "_" + codsecao + "_" + coddoc + "_" + dthrmov.getTime()
				+ "_" + (dthrultatu == null ? "0" : dthrultatu.getTime()) + "_"
				+ pagecount;
	}
}
