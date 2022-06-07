import com.seibel.lod.core.api.external.apiObjects.enums.DhApiFogDistance;
import com.seibel.lod.core.api.external.apiObjects.enums.DhApiVerticalQuality;
import com.seibel.lod.core.enums.config.VerticalQuality;
import com.seibel.lod.core.util.EnumUtil;
import org.junit.Test;
import org.junit.Assert;

/**
 * These tests were primary created to confirm that the
 * API enums are properly synced with their Core variants.
 *
 *
 * @author James Seibel
 * @version 2022-6-6
 */
public class ApiEnumSyncTests
{
	
	/** This is just a quick example to confirm the testing system is set up correctly. */
	@Test
	public void ExampleTests()
	{
		Assert.assertTrue("Example test 1", true);
		Assert.assertFalse("Example test 2", false);
	}
	
	/** Make sure each DhApi enum has the same values as its corresponding core enum. */
	@Test
	public void ConfirmEnumsAreSynced()
	{
		//================================//
		// base case tests to make sure   //
		// the tests are set up correctly //
		//================================//
		
		// this should always succeed (comparing the same enum to itself)
		AssertEnumsValuesAreEqual(VerticalQuality.class, VerticalQuality.class, true);
		// this should always fail (two completely different enums)
		AssertEnumsValuesAreEqual(VerticalQuality.class, DhApiFogDistance.class, false);
		
		
		
		//=========================//
		// actual enum comparisons //
		//=========================//
		
		// TODO using reflection I should be able to automatically find and compare each enum in the Api to its corresponding Core object
		AssertEnumsValuesAreEqual(VerticalQuality.class, DhApiVerticalQuality.class);
	}
	
	
	
	
	private <Ta extends Enum<Ta>, Tb extends Enum<Tb>> void AssertEnumsValuesAreEqual(Class<Ta> alphaEnum, Class<Tb> betaEnum)
	{
		AssertEnumsValuesAreEqual(alphaEnum, betaEnum, true);
	}
	private <Ta extends Enum<Ta>, Tb extends Enum<Tb>> void AssertEnumsValuesAreEqual(Class<Ta> alphaEnum, Class<Tb> betaEnum, boolean shouldBeEqual)
	{
		EnumUtil.EnumComparisonResult comparisonResult = EnumUtil.compareEnumsByValues(alphaEnum, betaEnum);
		
		if (shouldBeEqual)
		{
			Assert.assertTrue(comparisonResult.failMessage, comparisonResult.success);
		}
		else
		{
			Assert.assertFalse(comparisonResult.failMessage, comparisonResult.success);
		}
	}
	
}
