package com.clarkparsia.modularity.test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.clarkparsia.reachability.EntityNode;
import com.clarkparsia.reachability.ReachabilityGraph;
import com.clarkparsia.reachability.SCC;
import com.clarkparsia.owlapiv3.OWL;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Evren Sirin
 */
public class SCCTests {
	private ReachabilityGraph<OWLClass> graph;

	private EntityNode[]	nodes;

	private void addEdge(int in, int out) {
		nodes[in].addOutput( nodes[out] );
	}

	private void createGraph(int n) {
		graph = new ReachabilityGraph<OWLClass>();

		nodes = new EntityNode[n];
		for( int i = 0; i < n; i++ ) {
			nodes[i] = graph.createEntityNode( OWL.Class("entity" + i) );
		}
	}

	private void testSCC(int[][] expectedSCC) {
		List<Set<EntityNode<OWLClass>>> computed = SCC.computeSCC( graph );

		assertEquals( "SCC count", expectedSCC.length, computed.size() );

		for( int[] component : expectedSCC ) {
			Set<EntityNode> set = new HashSet<EntityNode>();
			for( int i : component ) {
				set.add( nodes[i] );
			}

			assertTrue( computed.contains( set ) );
		}
	}

	@Test
	public void simpleTest1() {
		createGraph( 8 );

		addEdge( 0, 1 );

		addEdge( 1, 2 );
		addEdge( 1, 4 );
		addEdge( 1, 5 );

		addEdge( 2, 3 );
		addEdge( 2, 6 );

		addEdge( 3, 2 );
		addEdge( 3, 7 );

		addEdge( 4, 0 );
		addEdge( 4, 5 );

		addEdge( 5, 6 );

		addEdge( 6, 5 );

		addEdge( 7, 3 );
		addEdge( 7, 6 );

		int[][] scc = { { 0, 1, 4 }, { 2, 3, 7 }, { 5, 6 } };

		testSCC( scc );
	}

	@Test
	public void simpleTest2() {
		createGraph( 11 );

		addEdge( 0, 1 );

		addEdge( 1, 2 );
		addEdge( 1, 3 );
		addEdge( 1, 4 );
		
		addEdge( 2, 5 );

		addEdge( 4, 1 );
		addEdge( 4, 6 );

		addEdge( 5, 2 );
		addEdge( 5, 7 );

		addEdge( 6, 7 );
		addEdge( 6, 8 );

		addEdge( 7, 10 );

		addEdge( 8, 9 );

		addEdge( 9, 6 );

		addEdge( 10, 8 );

		int[][] scc = { { 0 }, { 1, 4 }, { 3 }, { 2, 5 }, { 6, 7, 8, 9, 10 } };

		testSCC( scc );
	}

	@Test
	public void disconnectedTest() {
		createGraph( 7 );

		addEdge( 0, 1 );
		addEdge( 1, 2 );
		addEdge( 2, 3 );
		addEdge( 3, 2 );

		addEdge( 4, 5 );
		addEdge( 5, 6 );
		addEdge( 6, 4 );

		int[][] scc = { { 0 }, { 1 }, { 2, 3 }, { 4, 5, 6 } };

		testSCC( scc );
	}
}
