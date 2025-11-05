"""
Setup configuration for SensorVision SDK.
"""
from setuptools import setup, find_packages
from pathlib import Path

# Read the README file
this_directory = Path(__file__).parent
long_description = (this_directory / "README.md").read_text(encoding="utf-8")

setup(
    name="sensorvision-sdk",
    version="0.1.0",
    author="SensorVision Team",
    author_email="support@sensorvision.io",
    description="Python SDK for SensorVision - The IoT platform that scales with you",
    long_description=long_description,
    long_description_content_type="text/markdown",
    url="https://github.com/CodeFleck/sensorvision",
    project_urls={
        "Bug Tracker": "https://github.com/CodeFleck/sensorvision/issues",
        "Documentation": "https://github.com/CodeFleck/sensorvision/wiki",
        "Source Code": "https://github.com/CodeFleck/sensorvision",
    },
    packages=find_packages(exclude=["tests", "tests.*", "examples"]),
    classifiers=[
        "Development Status :: 4 - Beta",
        "Intended Audience :: Developers",
        "Topic :: Software Development :: Libraries :: Python Modules",
        "Topic :: System :: Monitoring",
        "Topic :: System :: Hardware",
        "License :: OSI Approved :: MIT License",
        "Programming Language :: Python :: 3",
        "Programming Language :: Python :: 3.8",
        "Programming Language :: Python :: 3.9",
        "Programming Language :: Python :: 3.10",
        "Programming Language :: Python :: 3.11",
        "Programming Language :: Python :: 3.12",
        "Operating System :: OS Independent",
    ],
    python_requires=">=3.8",
    install_requires=[
        "requests>=2.25.0",
    ],
    extras_require={
        "async": ["aiohttp>=3.8.0"],
        "dev": [
            "pytest>=7.0.0",
            "pytest-asyncio>=0.21.0",
            "pytest-cov>=4.0.0",
            "mypy>=1.0.0",
            "black>=23.0.0",
            "flake8>=6.0.0",
            "isort>=5.12.0",
        ],
        "raspberry-pi": ["Adafruit-DHT>=1.4.0"],
    },
    keywords=[
        "iot",
        "telemetry",
        "monitoring",
        "sensor",
        "raspberry-pi",
        "esp32",
        "sensorvision",
    ],
    include_package_data=True,
    zip_safe=False,
)
