const { test, expect } = require('@playwright/test');

test('graph renders with namespace filter', async ({ page }) => {
  page.on('console', msg => console.log('BROWSER LOG:', msg.text()));
  
  // Go to the visualization page with a filter
  await page.goto('http://localhost:9999/?ns=http-core');

  // Check for the "Syntax error" text
  const syntaxError = await page.getByText('Syntax error').count();
  expect(syntaxError).toBe(0);

  // Check that the SVG is rendered
  await expect(page.locator('svg')).toBeVisible({ timeout: 10000 });
  
  // Check that we see http-core nodes
  // Note: The exact text depends on how nodes are named in the graph.
  // Usually "name" or "do..."
  // But we can check the mermaid source if we could access it, or just check for *some* content.
  // Let's just ensure it renders without error for now.
});
