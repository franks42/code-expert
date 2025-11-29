const { test, expect } = require('@playwright/test');

test('UI Interaction Test', async ({ page }) => {
  // 1. Load the page
  await page.goto('http://localhost:9999');
  await expect(page).toHaveTitle(/Code Expert/);

  // 2. Check for controls
  const filterInput = page.locator('#ns-filter');
  const filterBtn = page.getByRole('button', { name: 'Filter' });
  const clearBtn = page.getByRole('button', { name: 'Clear' });

  await expect(filterInput).toBeVisible();
  await expect(filterBtn).toBeVisible();
  await expect(clearBtn).toBeVisible();

  // 3. Test Filtering
  await filterInput.fill('http-core');
  await filterBtn.click();
  
  // Verify URL updated
  await expect(page).toHaveURL(/ns=http-core/);
  
  // Verify input retains value
  await expect(filterInput).toHaveValue('http-core');

  // Verify graph loaded (wait for mermaid to render)
  await expect(page.locator('.mermaid svg')).toBeVisible();

  // 4. Test Clear
  await clearBtn.click();
  await expect(page).toHaveURL('http://localhost:9999/');
  await expect(filterInput).toHaveValue('');
});
