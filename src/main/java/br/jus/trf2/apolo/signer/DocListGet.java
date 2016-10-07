package br.jus.trf2.apolo.signer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import br.jus.trf2.assijus.system.api.IAssijusSystem.DocListGetRequest;
import br.jus.trf2.assijus.system.api.IAssijusSystem.DocListGetResponse;
import br.jus.trf2.assijus.system.api.IAssijusSystem.Document;
import br.jus.trf2.assijus.system.api.IAssijusSystem.IDocListGet;

public class DocListGet implements IDocListGet {

	@Override
	public void run(DocListGetRequest req, DocListGetResponse resp)
			throws Exception {

		// Parse request
		String cpf = req.cpf;

		// Setup json array
		List<Document> list = new ArrayList<>();
		// Get documents from Oracle
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rset = null;
		try {
			conn = Utils.getConnection();
			pstmt = conn.prepareStatement(Utils.getSQL("list"));
			pstmt.setString(1, cpf);
			pstmt.setString(2, cpf);
			pstmt.setString(3, cpf);
			pstmt.setString(4, cpf);
			rset = pstmt.executeQuery();

			while (rset.next()) {
				Document doc = new Document();
				Id id = new Id(cpf, rset.getInt("CODSECAO"),
						rset.getLong("CODDOC"),
						rset.getTimestamp("DATA_HORA_MOVIMENTO"), null, 0);
				doc.id = id.toString();
				doc.code = rset.getString("PROCESSO");
				doc.descr = rset.getString("MOTIVO");
				doc.kind = rset.getString("ATO");
				doc.origin = "Apolo";
				list.add(doc);
			}
		} finally {
			if (rset != null)
				rset.close();
			if (pstmt != null)
				pstmt.close();
			if (conn != null)
				conn.close();
		}

		resp.list = list;
	}

	@Override
	public String getContext() {
		return "listar documentos";
	}

}
