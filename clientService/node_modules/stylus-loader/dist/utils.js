"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.createEvaluator = createEvaluator;
exports.getStylusImplementation = getStylusImplementation;
exports.getStylusOptions = getStylusOptions;
exports.normalizeSourceMap = normalizeSourceMap;
exports.readFile = readFile;
exports.resolveFilename = resolveFilename;
exports.urlResolver = urlResolver;
var _nodePath = _interopRequireDefault(require("node:path"));
var _nodeUrl = require("node:url");
var _fastGlob = _interopRequireDefault(require("fast-glob"));
var _normalizePath = _interopRequireDefault(require("normalize-path"));
var _stylus = require("stylus");
var _depsResolver = _interopRequireDefault(require("stylus/lib/visitor/deps-resolver"));
function _interopRequireDefault(e) { return e && e.__esModule ? e : { default: e }; }
// eslint-disable-next-line n/no-deprecated-api

// Examples:
// - ~package
// - ~package/
// - ~@org
// - ~@org/
// - ~@org/package
// - ~@org/package/
const IS_MODULE_IMPORT = /^~([^/]+|[^/]+\/|@[^/]+[/][^/]+|@[^/]+\/?|@[^/]+[/][^/]+\/)$/;
const MODULE_REQUEST_REGEX = /^[^?]*~/;
function isProductionLikeMode(loaderContext) {
  return loaderContext.mode === "production" || !loaderContext.mode;
}
function getStylusOptions(loaderContext, loaderOptions) {
  const options = typeof loaderOptions.stylusOptions === "function" ? loaderOptions.stylusOptions(loaderContext) || {} : loaderOptions.stylusOptions || {};
  const stylusOptions = {
    filename: loaderContext.resourcePath,
    dest: _nodePath.default.dirname(loaderContext.resourcePath),
    ...options,
    // Keep track of imported files (used by Stylus CLI watch mode)

    // Don't allow to override, because it is internally
    _imports: []
  };
  if (typeof stylusOptions.use !== "undefined") {
    stylusOptions.use = (Array.isArray(stylusOptions.use) ? stylusOptions.use : [stylusOptions.use]).map(item => {
      if (typeof item === "string") {
        try {
          const resolved = require.resolve(item);
          loaderContext.addBuildDependency(resolved);
          return require(resolved)(stylusOptions);
        } catch (error) {
          throw new Error(`Failed to load "${item}" Stylus plugin. Are you sure it's installed?\n${error}`);
        }
      }
      return item;
    });
  }

  // https://github.com/stylus/stylus/issues/2119
  stylusOptions.resolveURL = typeof stylusOptions.resolveURL === "boolean" && !stylusOptions.resolveURL ? false : typeof stylusOptions.resolveURL === "object" ? {
    ...stylusOptions.resolveURL
  } : {
    nocheck: true
  };
  if (typeof stylusOptions.compress === "undefined" && isProductionLikeMode(loaderContext)) {
    stylusOptions.compress = true;
  }
  return stylusOptions;
}
function getStylusImplementation(loaderContext, implementation) {
  let resolvedImplementation = implementation;
  if (!implementation || typeof implementation === "string") {
    const stylusImplPkg = implementation || "stylus";
    resolvedImplementation = require(stylusImplPkg);
  }
  return resolvedImplementation;
}
function getPossibleRequests(loaderContext, filename) {
  let request = filename;

  // A `~` makes the url an module
  if (MODULE_REQUEST_REGEX.test(filename)) {
    request = request.replace(MODULE_REQUEST_REGEX, "");
  }
  if (IS_MODULE_IMPORT.test(filename)) {
    request = request[request.length - 1] === "/" ? request : `${request}/`;
  }
  return [...new Set([request, filename])];
}
async function resolveRequests(context, possibleRequests, resolve) {
  if (possibleRequests.length === 0) {
    throw new Error("Not found");
  }
  let result;
  try {
    result = await resolve(context, possibleRequests[0]);
  } catch (error) {
    const [, ...tailPossibleRequests] = possibleRequests;
    if (tailPossibleRequests.length === 0) {
      throw error;
    }
    result = await resolveRequests(context, tailPossibleRequests, resolve);
  }
  return result;
}
async function resolveFilename(loaderContext, fileResolver, globResolver, isGlob, context, filename) {
  const possibleRequests = getPossibleRequests(loaderContext, filename);
  let result;
  try {
    result = await resolveRequests(context, possibleRequests, fileResolver);
  } catch (error) {
    if (isGlob) {
      const [globTask] = _fastGlob.default.generateTasks(filename);
      if (globTask.base === ".") {
        throw new Error('Glob resolving without a glob base ("~**/*") is not supported, please specify a glob base ("~package/**/*")');
      }
      const possibleGlobRequests = getPossibleRequests(loaderContext, globTask.base);
      const globResult = await resolveRequests(context, possibleGlobRequests, globResolver);
      loaderContext.addContextDependency(globResult);
      const patterns = filename.replace(new RegExp(`^${globTask.base}`), (0, _normalizePath.default)(globResult));
      const paths = await (0, _fastGlob.default)(patterns, {
        absolute: true,
        cwd: globResult
      });
      return paths.sort().filter(file => /\.styl$/i.test(file));
    }
    throw error;
  }
  return result;
}
function readFile(inputFileSystem, filepath) {
  return new Promise((resolve, reject) => {
    inputFileSystem.readFile(filepath, (error, stats) => {
      if (error) {
        reject(error);
      }
      resolve(stats);
    });
  });
}
const URL_RE = /^(?:url\s*\(\s*)?['"]?(?:[#/]|(?:https?:)?\/\/)/i;
async function getDependencies(resolvedDependencies, loaderContext, fileResolver, globResolver, seen, code, filename, options) {
  seen.add(filename);

  // See https://github.com/stylus/stylus/issues/2108
  const newOptions = {
    ...options,
    filename,
    cache: false
  };
  const parser = new _stylus.Parser(code, newOptions);
  let ast;
  try {
    ast = parser.parse();
  } catch (error) {
    loaderContext.emitError(error);
    return;
  }
  const dependencies = [];
  class ImportVisitor extends _depsResolver.default {
    visitImport(node) {
      let firstNode = node.path.first;
      if (firstNode.name === "url") {
        return;
      }
      if (!firstNode.val) {
        const evaluator = new _stylus.Evaluator(ast);
        firstNode = evaluator.visit(firstNode).first;
      }
      const originalNodePath = !firstNode.val.isNull && firstNode.val || firstNode.name;
      let nodePath = originalNodePath;
      if (!nodePath) {
        return;
      }
      let found;
      let oldNodePath;
      const literal = /\.css(?:"|$)/.test(nodePath);
      if (!literal && !/\.styl$/i.test(nodePath)) {
        oldNodePath = nodePath;
        nodePath += ".styl";
      }
      const isGlob = _fastGlob.default.isDynamicPattern(nodePath);
      let {
        filename,
        paths
      } = this;
      if (_nodePath.default.sep === "\\") {
        filename = filename.replace(/^\\\\\?\\/, "");
        paths = paths.map(item => item.replace(/^\\\\\?\\/, ""));
      }
      found = _stylus.utils.find(nodePath, paths, filename);
      if (found && _nodePath.default.sep === "\\") {
        found = found.map(item => item.replace(/^\/\/\?\//, ""));
      }
      if (found && isGlob) {
        const [globTask] = _fastGlob.default.generateTasks(nodePath);
        const context = globTask.base === "." ? _nodePath.default.dirname(filename) : _nodePath.default.join(_nodePath.default.dirname(filename), globTask.base);
        loaderContext.addContextDependency(context);
      }
      if (!found && oldNodePath) {
        found = _stylus.utils.lookupIndex(oldNodePath, paths, filename);
        if (found && _nodePath.default.sep === "\\") {
          found = found.map(item => item.replace(/^\/\/\?\//, ""));
        }
      }
      if (found) {
        dependencies.push({
          originalLineno: firstNode.lineno,
          originalColumn: firstNode.column,
          originalNodePath,
          resolved: found.map(item => _nodePath.default.isAbsolute(item) ? item : _nodePath.default.join(process.cwd(), item))
        });
        return;
      }
      dependencies.push({
        originalLineno: firstNode.lineno,
        originalColumn: firstNode.column,
        originalNodePath,
        resolved: resolveFilename(loaderContext, fileResolver, globResolver, isGlob, _nodePath.default.dirname(filename), originalNodePath)
      });
    }
  }
  new ImportVisitor(ast, newOptions).visit(ast);
  await Promise.all([...dependencies].map(async result => {
    let {
      resolved
    } = result;
    try {
      resolved = await resolved;
    } catch (err) {
      delete result.resolved;
      result.error = err;
      return;
    }
    const isArray = Array.isArray(resolved);

    // `stylus` returns forward slashes on windows

    result.resolved = isArray ? resolved.map(item => _nodePath.default.normalize(item)) : _nodePath.default.normalize(resolved);
    const dependenciesOfDependencies = [];
    for (const dependency of isArray ? result.resolved : [result.resolved]) {
      // Avoid loop, the file is imported by itself
      if (seen.has(dependency)) {
        return;
      }

      // Avoid search nested imports in .css
      if (_nodePath.default.extname(dependency) === ".css") {
        return;
      }
      loaderContext.addDependency(dependency);
      dependenciesOfDependencies.push((async () => {
        let dependencyCode;
        try {
          dependencyCode = (await readFile(loaderContext.fs, dependency)).toString();
        } catch (error) {
          loaderContext.emitError(error);
        }
        await getDependencies(resolvedDependencies, loaderContext, fileResolver, globResolver, seen, dependencyCode, dependency, options);
      })());
    }
    await Promise.all(dependenciesOfDependencies);
  }));
  if (dependencies.length > 0) {
    resolvedDependencies.set(filename, dependencies);
  }
}
function mergeBlocks(blocks) {
  let finalBlock;
  for (const block of blocks) {
    if (finalBlock) {
      for (const node of block.nodes) {
        finalBlock.push(node);
      }
    } else {
      finalBlock = block;
    }
  }
  return finalBlock;
}
async function createEvaluator(loaderContext, code, options) {
  const fileResolve = loaderContext.getResolve({
    dependencyType: "stylus",
    conditionNames: ["styl", "stylus", "style", "..."],
    mainFields: ["styl", "style", "stylus", "main", "..."],
    mainFiles: ["index", "..."],
    extensions: [".styl", ".css"],
    restrictions: [/\.(css|styl)$/i],
    preferRelative: true
  });

  // Get cwd for `fastGlob()`
  // No need extra options, because they do not used when `resolveToContext` is `true`
  const globResolve = loaderContext.getResolve({
    conditionNames: ["styl", "stylus", "style", "..."],
    resolveToContext: true,
    preferRelative: true
  });
  const resolvedImportDependencies = new Map();
  const resolvedDependencies = new Map();
  const seen = new Set();
  await getDependencies(resolvedDependencies, loaderContext, fileResolve, globResolve, seen, code, loaderContext.resourcePath, options);
  const optionsImports = [];
  for (const importPath of options.imports) {
    const isGlob = _fastGlob.default.isDynamicPattern(importPath);
    optionsImports.push({
      importPath,
      resolved: resolveFilename(loaderContext, fileResolve, globResolve, isGlob, _nodePath.default.dirname(loaderContext.resourcePath), importPath)
    });
  }
  await Promise.all(optionsImports.map(async result => {
    const {
      importPath
    } = result;
    let {
      resolved
    } = result;
    try {
      resolved = await resolved;
    } catch {
      return;
    }
    const isArray = Array.isArray(resolved);

    // `stylus` returns forward slashes on windows

    result.resolved = isArray ? resolved.map(item => _nodePath.default.normalize(item)) : _nodePath.default.normalize(resolved);
    resolvedImportDependencies.set(importPath, result);
    const dependenciesOfImportDependencies = [];
    for (const dependency of isArray ? result.resolved : [result.resolved]) {
      dependenciesOfImportDependencies.push((async () => {
        let dependencyCode;
        try {
          dependencyCode = (await readFile(loaderContext.fs, dependency)).toString();
        } catch (error) {
          loaderContext.emitError(error);
        }
        await getDependencies(resolvedDependencies, loaderContext, fileResolve, globResolve, seen, dependencyCode, dependency, options);
      })());
    }
    await Promise.all(dependenciesOfImportDependencies);
  }));
  return class CustomEvaluator extends _stylus.Evaluator {
    visitImport(imported) {
      this.return += 1;
      const node = this.visit(imported.path).first;
      const nodePath = !node.val.isNull && node.val || node.name;
      this.return -= 1;
      let webpackResolveError;
      if (node.name !== "url" && nodePath && !URL_RE.test(nodePath)) {
        let dependency;
        let {
          filename
        } = node;
        if (_nodePath.default.sep === "\\") {
          filename = filename.replace(/^\/\/\?\//, "");
        }
        const isEntrypoint = loaderContext.resourcePath === filename;
        if (isEntrypoint) {
          dependency = resolvedImportDependencies.get(nodePath);
        }
        if (!dependency) {
          const dependencies = resolvedDependencies.get(_nodePath.default.normalize(filename));
          if (dependencies) {
            dependency = dependencies.find(item => {
              if (item.originalLineno === node.lineno && item.originalColumn === node.column && item.originalNodePath === nodePath) {
                if (item.error) {
                  webpackResolveError = item.error;
                } else {
                  return item.resolved;
                }
              }
              return false;
            });
          }
        }
        if (dependency) {
          const {
            resolved
          } = dependency;
          if (!Array.isArray(resolved)) {
            // Avoid re globbing when resolved import contains glob characters
            node.string = _fastGlob.default.escapePath(resolved);
          } else if (resolved.length > 0) {
            let hasError = false;
            const blocks = resolved.map(item => {
              const clonedImported = imported.clone();
              const clonedNode = this.visit(clonedImported.path).first;

              // Avoid re globbing when resolved import contains glob characters
              clonedNode.string = _fastGlob.default.escapePath(item);
              let result;
              try {
                result = super.visitImport(clonedImported);
              } catch {
                hasError = true;
              }
              return result;
            });
            if (!hasError) {
              return mergeBlocks(blocks);
            }
          }
        }
      }
      let result;
      try {
        result = super.visitImport(imported);
      } catch (error) {
        loaderContext.emitError(new Error(`Stylus resolver error: ${error.message}${webpackResolveError ? `\n\nWebpack resolver error: ${webpackResolveError.message}${webpackResolveError.details ? `\n\nWebpack resolver error details:\n${webpackResolveError.details}` : ""}${webpackResolveError.missing ? `\n\nWebpack resolver error missing:\n${webpackResolveError.missing.join("\n")}` : ""}` : ""}`));
        return imported;
      }
      return result;
    }
  };
}
function urlResolver(options = {}) {
  function resolver(url) {
    const compiler = new _stylus.Compiler(url);
    let {
      filename
    } = url;
    if (_nodePath.default.sep === "\\") {
      filename = filename.replace(/^\/\/\?\//, "");
    }
    compiler.isURL = true;
    const visitedUrl = url.nodes.map(node => compiler.visit(node)).join("");
    const splitted = visitedUrl.split("!");
    const parsedUrl = (0, _nodeUrl.parse)(splitted.pop());

    // Parse literal
    const literal = new _stylus.nodes.Literal(`url("${parsedUrl.href}")`);
    let {
      pathname
    } = parsedUrl;
    let {
      dest
    } = this.options;
    let tail = "";
    let res;

    // Absolute or hash
    if (parsedUrl.protocol || !pathname || pathname[0] === "/") {
      return literal;
    }

    // Check that file exists
    if (!options.nocheck) {
      const _paths = options.paths || [];
      pathname = _stylus.utils.lookup(pathname, [..._paths, ...(_nodePath.default.sep === "\\" ? this.paths.map(item => _nodePath.default.normalize(item.replace(/^\/\/\?\//, ""))) : this.paths)]);
      if (_nodePath.default.sep === "\\") {
        pathname = pathname.replace(/^\\\\\?\\/, "");
      }
      if (!pathname) {
        return literal;
      }
    }
    if (this.includeCSS && _nodePath.default.extname(pathname) === ".css") {
      return new _stylus.nodes.Literal(parsedUrl.href);
    }
    if (parsedUrl.search) {
      tail += parsedUrl.search;
    }
    if (parsedUrl.hash) {
      tail += parsedUrl.hash;
    }
    if (dest && _nodePath.default.extname(dest) === ".css") {
      dest = _nodePath.default.dirname(dest);
    }
    res = _nodePath.default.relative(dest || _nodePath.default.dirname(this.filename), options.nocheck ? _nodePath.default.join(_nodePath.default.dirname(filename), pathname) : pathname) + tail;
    if (_nodePath.default.sep === "\\") {
      res = (0, _normalizePath.default)(res);
    }
    splitted.push(res);
    return new _stylus.nodes.Literal(`url("${splitted.join("!")}")`);
  }
  resolver.options = options;
  resolver.raw = true;
  return resolver;
}
const IS_NATIVE_WIN32_PATH = /^[a-z]:[/\\]|^\\\\/i;
const ABSOLUTE_SCHEME = /^[A-Za-z0-9+\-.]+:/;
function getURLType(source) {
  if (source[0] === "/") {
    if (source[1] === "/") {
      return "scheme-relative";
    }
    return "path-absolute";
  }
  if (IS_NATIVE_WIN32_PATH.test(source)) {
    return "path-absolute";
  }
  return ABSOLUTE_SCHEME.test(source) ? "absolute" : "path-relative";
}
function normalizeSourceMap(map, rootContext) {
  const newMap = map;

  // result.map.file is an optional property that provides the output filename.
  // Since we don't know the final filename in the webpack build chain yet, it makes no sense to have it.

  delete newMap.file;
  newMap.sourceRoot = "";
  newMap.sources = newMap.sources.map(source => {
    if (_nodePath.default.sep === "\\") {
      source = _nodePath.default.normalize(source.replace(/^\/\/\?\//, ""));
    }
    const sourceType = getURLType(source);

    // Do no touch `scheme-relative`, `path-absolute` and `absolute` types
    if (sourceType === "path-relative") {
      return _nodePath.default.resolve(rootContext, _nodePath.default.normalize(source));
    }
    return source;
  });
  return newMap;
}