package com.complexible.pellet.client.reasoner;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.clarkparsia.owlapiv3.OWL;
import com.clarkparsia.pellet.service.reasoner.SchemaQuery;
import com.clarkparsia.pellet.service.reasoner.SchemaReasoner;
import com.complexible.pellet.client.ClientTools;
import com.complexible.pellet.client.PelletService;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.NodeSet;
import retrofit2.Call;


/**
 * Implementation of a {@link SchemaReasoner} using the Pellet Service API remote reasoner.
 *
 * @author Edgar Rodriguez-Diaz
 */
public class RemoteSchemaReasoner implements SchemaReasoner {

	final PelletService pelletService;
	final IRI ontologyIri;

	private static final UUID CLIENT_ID = UUID.randomUUID();

	private LoadingCache<SchemaQuery, NodeSet<?>> cache = CacheBuilder.newBuilder()
		                   .maximumSize(1024)
		                   .build(new CacheLoader<SchemaQuery, NodeSet<?>>() {
			                   @Override
			                   public NodeSet<?> load(final SchemaQuery query) throws Exception {
				                   return executeRemoteQuery(query);
			                   }
		                   });


	@SuppressWarnings("unused")
	@Inject
	public RemoteSchemaReasoner(final PelletService pelletService,
	                            @Assisted final OWLOntology ontology) {
		this.pelletService = pelletService;
		this.ontologyIri = ontology.getOntologyID().getOntologyIRI().get();
	}

	@Override
	public <T extends OWLObject> NodeSet<T> query(final SchemaQuery query) {
		try {
			return (NodeSet<T>) cache.get(query);
		}
		catch (Exception e) {
			Throwables.propagate(e);
		}
		return null;
	}

	private <T extends OWLObject> NodeSet<T> executeRemoteQuery(final SchemaQuery query) {
		Call<NodeSet> queryCall = pelletService.query(ontologyIri, CLIENT_ID, query);
		return ClientTools.executeCall(queryCall);
	}

	@Override
	public Set<Set<OWLAxiom>> explain(final OWLAxiom inference, final int limit) {
		try {
			Call<OWLOntology> explainCall = pelletService.explain(ontologyIri, CLIENT_ID, limit, inference);
			final OWLOntology ont = ClientTools.executeCall(explainCall);

			final OWLAnnotationProperty label = ont.getOWLOntologyManager().getOWLDataFactory().getRDFSLabel();
			Map<String, Set<OWLAxiom>> explanations = Maps.newHashMap();
			for (OWLAxiom axiom : ont.getLogicalAxioms()) {
				OWLAnnotation annotation = axiom.getAnnotations(label).iterator().next();
				String explanationId = ((OWLLiteral) annotation.getValue()).getLiteral();
				Set<OWLAxiom> explanation = explanations.get(explanationId);
				if (explanation == null) {
					explanation = Sets.newHashSet();
					explanations.put(explanationId, explanation);
				}
				explanation.add(axiom.getAxiomWithoutAnnotations());
			}

			return Sets.newHashSet(explanations.values());
		}
		catch (Exception e) {
			Throwables.propagate(e);
		}
		return null;
	}

	@Override
	public void classify() {
		try {
			ClientTools.executeCall(pelletService.classify(ontologyIri, CLIENT_ID));
		}
		catch (Exception e) {
			Throwables.propagate(e);
		}
	}

	@Override
	public void insert(Set<OWLAxiom> additions) {
		try {
			cache.invalidateAll();
			ClientTools.executeCall(pelletService.insert(ontologyIri, CLIENT_ID, OWL.Ontology(additions)));
		}
		catch (Exception e) {
			Throwables.propagate(e);
		}
	}

	@Override
	public void delete(Set<OWLAxiom> removals) {
		try {
			cache.invalidateAll();
			ClientTools.executeCall(pelletService.delete(ontologyIri, CLIENT_ID, OWL.Ontology(removals)));
		}
		catch (Exception e) {
			Throwables.propagate(e);
		}
	}

	@Override
	public void close() throws Exception {
		cache.invalidateAll();
	}
}
