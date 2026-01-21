/**
 * Converts snake_case strings to camelCase.
 *
 * This is needed because widgets store variable names in snake_case
 * (e.g., "kw_consumption") but the API returns data with camelCase
 * property names (e.g., "kwConsumption") due to Jackson serialization.
 *
 * @example
 * toCamelCase('kw_consumption') // => 'kwConsumption'
 * toCamelCase('power_factor') // => 'powerFactor'
 * toCamelCase('temperature') // => 'temperature' (unchanged)
 *
 * @param str - The snake_case string to convert
 * @returns The camelCase version of the string
 */
export const toCamelCase = (str: string): string => {
  return str.replace(/_([a-z])/g, (_, letter) => letter.toUpperCase());
};

/**
 * Safely retrieves a numeric value from telemetry data,
 * trying both camelCase and snake_case property names.
 *
 * This provides a consistent fallback pattern for all widgets
 * to handle the naming convention mismatch between widget config
 * and API responses.
 *
 * @example
 * getTelemetryValue({ kwConsumption: 42.5 }, 'kw_consumption') // => 42.5
 * getTelemetryValue({ kw_consumption: 42.5 }, 'kw_consumption') // => 42.5
 * getTelemetryValue({ other: 100 }, 'kw_consumption') // => undefined
 *
 * @param data - The telemetry data object
 * @param varName - The variable name (typically snake_case from widget config)
 * @returns The numeric value if found, undefined otherwise
 */
export const getTelemetryValue = (
  data: Record<string, unknown>,
  varName: string
): number | undefined => {
  const camelName = toCamelCase(varName);

  // Try camelCase first (API standard from Jackson serialization)
  if (typeof data[camelName] === 'number') {
    return data[camelName] as number;
  }

  // Fallback to snake_case (in case API changes or for dynamic variables)
  if (typeof data[varName] === 'number') {
    return data[varName] as number;
  }

  return undefined;
};
